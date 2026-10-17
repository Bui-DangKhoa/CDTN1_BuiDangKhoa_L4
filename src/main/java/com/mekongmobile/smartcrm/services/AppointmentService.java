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

/**
 * TẦNG 2: Dịch vụ nghiệp vụ quản lý lịch hẹn kỹ thuật viên (Appointment).
 * Xử lý quy tắc chống trùng lịch hẹn (QT-L4-01 / NFR2).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final TicketRepository ticketRepository;
    private final TechnicianRepository technicianRepository;

    /**
     * QT-L4-01 / NFR2: Đặt lịch hẹn mới cho KTV và kiểm tra chống trùng lịch trong cùng khung giờ.
     */
    public Appointment scheduleAppointment(Long ticketId, Long technicianId, LocalDateTime appointmentTime) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException("NOT_FOUND", "Không tìm thấy phiếu yêu cầu với ID: " + ticketId));

        Technician technician = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new AppException("NOT_FOUND", "Không tìm thấy kỹ thuật viên với ID: " + technicianId));

        // QT-L4-01 / NFR2: Kiểm tra chống gán trùng lịch hẹn cho KTV trong cùng khung giờ
        boolean hasConflict = appointmentRepository.existsByTechnician_TechnicianIdAndAppointmentTimeAndStatusNot(
                technicianId, appointmentTime, "DA_HUY");

        if (hasConflict) {
            throw new AppException("SCHEDULE_CONFLICT", "KTV đã có lịch hẹn khác trong cùng khung giờ này");
        }

        Appointment appointment = Appointment.builder()
                .ticket(ticket)
                .technician(technician)
                .appointmentTime(appointmentTime)
                .status("CHO_XAC_NHAN")
                .createdAt(LocalDateTime.now())
                .build();

        return appointmentRepository.save(appointment);
    }

    /**
     * Hủy lịch hẹn và giải phóng khung giờ cho KTV.
     */
    public Appointment cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException("NOT_FOUND", "Không tìm thấy lịch hẹn với ID: " + appointmentId));

        appointment.setStatus("DA_HUY");
        return appointmentRepository.save(appointment);
    }
}
