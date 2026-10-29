package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.entities.Appointment;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.entities.Ticket;
import com.mekongmobile.smartcrm.exceptions.AppException;
import com.mekongmobile.smartcrm.repositories.AppointmentRepository;
import com.mekongmobile.smartcrm.repositories.TechnicianRepository;
import com.mekongmobile.smartcrm.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
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
@lombok.extern.slf4j.Slf4j
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
                .orElseThrow(() -> new AppException("NOT_FOUND", "Không tìm thấy phiếu yêu cầu với ID: " + ticketId));

        Technician technician = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new AppException("NOT_FOUND", "Không tìm thấy kỹ thuật viên với ID: " + technicianId));

        return scheduleAppointment(ticket, technician, appointmentTime);
    }

    /**
     * QT-L4-01 / NFR2: Đặt lịch hẹn cho KTV khi đã có sẵn thực thể Ticket và Technician
     * (dùng chung trong transaction phân công KTV của {@code TicketServiceImpl.assignTechnician}).
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
     * Hủy lịch hẹn và giải phóng khung giờ cho KTV.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveAppointment(Long ticketId) {
        if (ticketId == null) {
            return false;
        }
        return appointmentRepository.existsByTicket_TicketIdAndStatusNot(ticketId, "DA_HUY");
    }

    public Appointment cancelAppointment(Long appointmentId) {
        log.info("Tiếp nhận yêu cầu hủy lịch hẹn ID: {}", appointmentId);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException("NOT_FOUND", "Không tìm thấy lịch hẹn với ID: " + appointmentId));

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
                .orElseThrow(() -> new AppException("NOT_FOUND", "Không tìm thấy lịch hẹn với ID: " + appointmentId));

        appointment.setStatus("DA_XAC_NHAN");
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Xác nhận lịch hẹn ID: {} thành công (US7)", appointmentId);
        return saved;
    }

    /**
     * US7 / FR7: Tra cứu thông tin lịch hẹn theo mã phiếu bảo hành hoặc SĐT khách hàng.
     */
    /**
     * US7 / FR7 + QT-15: Chuyển đổi Appointment sang AppointmentResponseDto,
     * che 4 số giữa SĐT khách hàng qua MaskingUtil tại ranh giới DTO/Mapper.
     */
    public com.mekongmobile.smartcrm.dto.AppointmentResponseDto mapToDto(Appointment app) {
        if (app == null) {
            return null;
        }
        Ticket ticket = app.getTicket();
        Technician tech = app.getTechnician();
        String rawPhone = (ticket != null) ? ticket.getCustomerPhone() : null;
        String maskedPhone = com.mekongmobile.smartcrm.config.MaskingUtil.maskPhoneNumber(rawPhone);

        return com.mekongmobile.smartcrm.dto.AppointmentResponseDto.builder()
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
    public List<com.mekongmobile.smartcrm.dto.AppointmentResponseDto> findAppointmentsByKeyword(String keyword) {
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
    public com.mekongmobile.smartcrm.dto.AppointmentResponseDto createAppointment(com.mekongmobile.smartcrm.dto.CreateAppointmentRequest request) {
        Appointment appointment = scheduleAppointment(request.getTicketId(), request.getTechnicianId(), request.getAppointmentTime());
        return com.mekongmobile.smartcrm.dto.AppointmentResponseDto.builder()
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

