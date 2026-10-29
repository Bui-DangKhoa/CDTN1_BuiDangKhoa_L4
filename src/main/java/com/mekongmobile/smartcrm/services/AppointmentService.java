package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.dto.AppointmentResponseDto;
import com.mekongmobile.smartcrm.dto.CreateAppointmentRequest;
import com.mekongmobile.smartcrm.dto.UpdateAppointmentRequest;
import com.mekongmobile.smartcrm.entities.Appointment;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.entities.Ticket;
import com.mekongmobile.smartcrm.exceptions.AppException;
import com.mekongmobile.smartcrm.repositories.AppointmentRepository;
import com.mekongmobile.smartcrm.repositories.TechnicianRepository;
import com.mekongmobile.smartcrm.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TẦNG 2: Dịch vụ nghiệp vụ quản lý lịch hẹn kỹ thuật viên (Appointment).
 * Xử lý quy tắc chống trùng lịch hẹn (QT-L4-01 / NFR2).
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final TicketRepository ticketRepository;
    private final TechnicianRepository technicianRepository;

    /**
     * QT-L4-01 / NFR2: Đặt lịch hẹn mới cho KTV và kiểm tra chống trùng lịch trong cùng khung giờ.
     */
    public Appointment scheduleAppointment(Long ticketId, Long technicianId, LocalDateTime appointmentTime) {
        log.info("Bắt đầu đặt lịch hẹn cho ticketId: {}, technicianId: {}, appointmentTime: {}",
                ticketId, technicianId, appointmentTime);

        Ticket ticket = ticketRepository.findById(ticketId)
                .filter(t -> t.getDeletedAt() == null)
                .orElseThrow(() -> new AppException("TICKET_NOT_FOUND", "Không tìm thấy phiếu yêu cầu với ID: " + ticketId));

        Technician technician = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new AppException("NOT_FOUND", "Không tìm thấy kỹ thuật viên với ID: " + technicianId));

        return scheduleAppointment(ticket, technician, appointmentTime);
    }

    /**
     * QT-L4-01 / NFR2: Đặt lịch hẹn cho KTV khi đã có sẵn thực thể Ticket và Technician
     * (dùng chung trong transaction phân công KTV của TicketServiceImpl.assignTechnician).
     */
    public Appointment scheduleAppointment(Ticket ticket, Technician technician, LocalDateTime appointmentTime) {
        if (appointmentTime == null) {
            throw new AppException("INVALID_APPOINTMENT_TIME", "Vui lòng chọn ngày và khung giờ hẹn");
        }
        Long technicianId = technician.getTechnicianId();

        // QT-L4-01 / NFR2: Kiểm tra chống gán trùng lịch hẹn cho KTV trong cùng khung giờ
        boolean hasConflict = appointmentRepository.existsByTechnician_TechnicianIdAndAppointmentTimeAndStatusNot(
                technicianId, appointmentTime, "DA_HUY");

        if (hasConflict) {
            log.warn("Vi phạm quy tắc chống trùng lịch QT-L4-01: KTV {} (ID: {}) đã có lịch hẹn khác tại khung giờ {}",
                    technician.getFullName(), technicianId, appointmentTime);
            throw new AppException("SCHEDULE_CONFLICT", "KTV đã có lịch hẹn khác trong cùng khung giờ này");
        }

        Appointment appointment = Appointment.builder()
                .ticket(ticket)
                .technician(technician)
                .appointmentTime(appointmentTime)
                .status("CHO_XAC_NHAN")
                .createdAt(LocalDateTime.now())
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Đặt lịch hẹn thành công (ID: {}) cho KTV {} tại thời điểm {} (QT-L4-01)",
                savedAppointment.getAppointmentId(), technician.getFullName(), appointmentTime);
        return savedAppointment;
    }

    /**
     * Tra cứu chi tiết lịch hẹn theo ID.
     */
    @Transactional(readOnly = true)
    public AppointmentResponseDto getAppointmentById(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException("APPOINTMENT_NOT_FOUND", "Không tìm thấy lịch hẹn với ID: " + appointmentId));
        return mapToDto(appointment);
    }

    /**
     * Tra cứu danh sách lịch hẹn (lọc theo ticketId hoặc technicianId nếu có).
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> getAllAppointments(Long ticketId, Long technicianId) {
        List<Appointment> list;
        if (ticketId != null) {
            list = appointmentRepository.findByTicket_TicketId(ticketId);
        } else if (technicianId != null) {
            list = appointmentRepository.findByTechnician_TechnicianId(technicianId);
        } else {
            list = appointmentRepository.findAll();
        }
        return list.stream().map(this::mapToDto).toList();
    }

    /**
     * Cập nhật thông tin lịch hẹn (Appointment PUT).
     * Kiểm tra chống trùng lịch QT-L4-01 trong khung giờ ±30 phút, LOẠI TRỪ chính appointmentId đang cập nhật.
     */
    @Transactional
    public AppointmentResponseDto updateAppointment(Long appointmentId, UpdateAppointmentRequest request) {
        log.info("Cập nhật lịch hẹn ID: {}, request: {}", appointmentId, request);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException("APPOINTMENT_NOT_FOUND", "Không tìm thấy lịch hẹn với ID: " + appointmentId));

        if ("DA_HUY".equalsIgnoreCase(appointment.getStatus())) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Không thể cập nhật lịch hẹn đã bị hủy");
        }

        Long technicianId = (request.getTechnicianId() != null)
                ? request.getTechnicianId()
                : appointment.getTechnician().getTechnicianId();

        Technician technician = appointment.getTechnician();
        if (request.getTechnicianId() != null && !request.getTechnicianId().equals(technician.getTechnicianId())) {
            technician = technicianRepository.findById(technicianId)
                    .orElseThrow(() -> new AppException("NOT_FOUND", "Không tìm thấy kỹ thuật viên với ID: " + technicianId));
            appointment.setTechnician(technician);
        }

        LocalDateTime newTime = request.getAppointmentTime();
        if (newTime == null) {
            throw new AppException("INVALID_APPOINTMENT_TIME", "Thời gian hẹn không được để trống");
        }

        // QT-L4-01: Kiểm tra xung đột trong khung giờ ±30 phút, loại trừ chính appointmentId đang cập nhật
        LocalDateTime windowStart = newTime.minusMinutes(30);
        LocalDateTime windowEnd = newTime.plusMinutes(30);

        boolean hasConflict = appointmentRepository.existsActiveAppointmentInWindowExcludingSelf(
                technicianId, windowStart, windowEnd, appointmentId);

        if (hasConflict) {
            log.warn("Vi phạm quy tắc QT-L4-01: KTV {} đã có lịch hẹn khác trong khoảng ±30 phút quanh {}",
                    technician.getFullName(), newTime);
            throw new AppException("SCHEDULE_CONFLICT", "KTV đã có lịch hẹn khác trong khung giờ này");
        }

        appointment.setAppointmentTime(newTime);
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Cập nhật lịch hẹn ID {} thành công (QT-L4-01)", saved.getAppointmentId());
        return mapToDto(saved);
    }

    /**
     * Hủy toàn bộ lịch hẹn còn hiệu lực của một phiếu (dùng khi đổi KTV – QT-07),
     * giải phóng khung giờ của KTV cũ trước khi tạo lịch hẹn mới.
     */
    public void cancelActiveAppointmentsForTicket(Long ticketId) {
        appointmentRepository.findByTicket_TicketId(ticketId).stream()
                .filter(a -> !"DA_HUY".equalsIgnoreCase(a.getStatus()))
                .forEach(a -> {
                    a.setStatus("DA_HUY");
                    appointmentRepository.save(a);
                    log.info("Hủy lịch hẹn cũ ID: {} của phiếu ID: {} do phân công lại KTV", a.getAppointmentId(), ticketId);
                });
    }

    /**
     * Kiểm tra phiếu bảo hành có lịch hẹn nào đang còn hiệu lực hay không.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveAppointment(Long ticketId) {
        if (ticketId == null) {
            return false;
        }
        return appointmentRepository.existsByTicket_TicketIdAndStatusNot(ticketId, "DA_HUY");
    }

    /**
     * Hủy lịch hẹn và giải phóng khung giờ cho KTV (hủy mềm status = DA_HUY).
     * Chỉ cho phép hủy khi lịch hẹn chưa ở trạng thái DA_XAC_NHAN, nếu không ném APPOINTMENT_NOT_DELETABLE.
     */
    public Appointment cancelAppointment(Long appointmentId) {
        log.info("Tiếp nhận yêu cầu hủy lịch hẹn ID: {}", appointmentId);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException("APPOINTMENT_NOT_FOUND", "Không tìm thấy lịch hẹn với ID: " + appointmentId));

        if ("DA_XAC_NHAN".equalsIgnoreCase(appointment.getStatus())) {
            log.warn("Không thể hủy lịch hẹn ID {} vì đã ở trạng thái DA_XAC_NHAN", appointmentId);
            throw new AppException("APPOINTMENT_NOT_DELETABLE", "Không thể hủy lịch hẹn đã được khách hàng xác nhận");
        }

        appointment.setStatus("DA_HUY");
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Hủy lịch hẹn ID: {} thành công, đã giải phóng khung giờ cho KTV", appointmentId);
        return saved;
    }

    /**
     * US7 / FR7: Khách hàng xác nhận lịch hẹn (chuyển trạng thái sang DA_XAC_NHAN).
     */
    public Appointment confirmAppointment(Long appointmentId) {
        log.info("Khách hàng xác nhận lịch hẹn ID: {}", appointmentId);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException("APPOINTMENT_NOT_FOUND", "Không tìm thấy lịch hẹn với ID: " + appointmentId));

        appointment.setStatus("DA_XAC_NHAN");
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Xác nhận lịch hẹn ID: {} thành công (US7)", appointmentId);
        return saved;
    }

    /**
     * US7 / FR7 + QT-15: Chuyển đổi Appointment sang AppointmentResponseDto,
     * che 4 số giữa SĐT khách hàng qua MaskingUtil tại ranh giới DTO/Mapper.
     */
    public AppointmentResponseDto mapToDto(Appointment app) {
        if (app == null) {
            return null;
        }
        Ticket ticket = app.getTicket();
        Technician tech = app.getTechnician();
        String rawPhone = (ticket != null) ? ticket.getCustomerPhone() : null;
        String maskedPhone = com.mekongmobile.smartcrm.config.MaskingUtil.maskPhoneNumber(rawPhone);

        return AppointmentResponseDto.builder()
                .appointmentId(app.getAppointmentId())
                .ticketId(ticket != null ? ticket.getTicketId() : null)
                .ticketCode(ticket != null ? ticket.getTicketCode() : null)
                .technicianId(tech != null ? tech.getTechnicianId() : null)
                .technicianName(tech != null ? tech.getFullName() : null)
                .appointmentTime(app.getAppointmentTime())
                .status(app.getStatus())
                .createdAt(app.getCreatedAt())
                .customerPhone(maskedPhone)
                .deviceName(ticket != null && ticket.getDevice() != null ? ticket.getDevice().getDeviceName() : null)
                .issueDesc(ticket != null ? ticket.getIssueDesc() : null)
                .ticket(ticket)
                .technician(tech)
                .build();
    }

    /**
     * US7 / FR7: Tra cứu thông tin lịch hẹn theo mã phiếu bảo hành hoặc SĐT khách hàng.
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> findAppointmentsByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return appointmentRepository.findByTicketCodeOrPhone(keyword.trim())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Đặt lịch hẹn tiếp nhận DTO từ Web UI / REST API.
     */
    public AppointmentResponseDto createAppointment(CreateAppointmentRequest request) {
        Appointment appointment = scheduleAppointment(request.getTicketId(), request.getTechnicianId(), request.getAppointmentTime());
        return AppointmentResponseDto.builder()
                .appointmentId(appointment.getAppointmentId())
                .ticketId(appointment.getTicket().getTicketId())
                .ticketCode(appointment.getTicket().getTicketCode())
                .technicianId(appointment.getTechnician().getTechnicianId())
                .technicianName(appointment.getTechnician().getFullName())
                .appointmentTime(appointment.getAppointmentTime())
                .status(appointment.getStatus())
                .createdAt(appointment.getCreatedAt())
                .build();
    }
}
