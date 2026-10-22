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
                .orElseThrow(() -> new AppException("NOT_FOUND", "Không tìm thấy phiếu yêu cầu với ID: " + ticketId));

        Technician technician = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new AppException("NOT_FOUND", "Không tìm thấy kỹ thuật viên với ID: " + technicianId));

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
     * Hủy lịch hẹn và giải phóng khung giờ cho KTV.
     */
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
    @Transactional(readOnly = true)
    public List<Appointment> findAppointmentsByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return appointmentRepository.findByTicketCodeOrPhone(keyword.trim());
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

