package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.dto.ScheduleAppointmentDto;
import com.mekongmobile.smartcrm.dto.TechnicianDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.entities.Appointment;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.entities.Ticket;
import com.mekongmobile.smartcrm.entities.TicketStatusLog;
import com.mekongmobile.smartcrm.exceptions.BusinessRuleException;
import com.mekongmobile.smartcrm.exceptions.ResourceNotFoundException;
import com.mekongmobile.smartcrm.repositories.AppointmentRepository;
import com.mekongmobile.smartcrm.repositories.TechnicianRepository;
import com.mekongmobile.smartcrm.repositories.TicketRepository;
import com.mekongmobile.smartcrm.repositories.TicketStatusLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * TẦNG 2: Cài đặt logic nghiệp vụ Điều phối kỹ thuật viên (QT-07).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SchedulingServiceImpl implements SchedulingService {

    private final TicketRepository ticketRepository;
    private final TechnicianRepository technicianRepository;
    private final AppointmentRepository appointmentRepository;
    private final TicketStatusLogRepository statusLogRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TechnicianDto> findEligibleTechnicians(Long ticketId, LocalDateTime appointmentTime) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .filter(t -> t.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu yêu cầu (Ticket)", ticketId));

        Long centerId = ticket.getCenterId();
        Integer categoryId = ticket.getCategory().getCategoryId();

        // Lấy danh sách kỹ thuật viên cùng trung tâm và có kỹ năng xử lý loại sự cố này
        List<Technician> skilledTechnicians = technicianRepository.findEligibleTechnicians(centerId, categoryId);

        // Lọc kỹ thuật viên không bị trùng lịch tại thời điểm appointmentTime
        return skilledTechnicians.stream()
                .filter(tech -> !appointmentRepository.existsActiveAppointmentAtTime(tech.getTechnicianId(), appointmentTime))
                .map(this::mapTechnicianToDto)
                .toList();
    }

    @Override
    public TicketResponseDto scheduleAppointment(ScheduleAppointmentDto scheduleDto) {
        Ticket ticket = ticketRepository.findById(scheduleDto.getTicketId())
                .filter(t -> t.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu yêu cầu (Ticket)", scheduleDto.getTicketId()));

        Technician technician = technicianRepository.findById(scheduleDto.getTechnicianId())
                .orElseThrow(() -> new ResourceNotFoundException("Kỹ thuật viên", scheduleDto.getTechnicianId()));

        // Kiểm tra trạng thái hoạt động của kỹ thuật viên
        if (Boolean.FALSE.equals(technician.getIsActive())) {
            throw new BusinessRuleException("Kỹ thuật viên " + technician.getFullName() + " hiện đang không hoạt động (Inactive)");
        }

        // Kiểm tra trung tâm dịch vụ tương ứng
        if (!Objects.equals(ticket.getCenterId(), technician.getCenterId())) {
            throw new BusinessRuleException("Kỹ thuật viên không thuộc cùng trung tâm dịch vụ với ticket");
        }

        // QT-07: Kiểm tra xung đột lịch hẹn (Conflict Detection)
        boolean hasConflict = appointmentRepository.existsActiveAppointmentAtTime(
                technician.getTechnicianId(), scheduleDto.getAppointmentTime());
        if (hasConflict) {
            throw new BusinessRuleException("Kỹ thuật viên đã có lịch hẹn khác tại thời điểm: " + scheduleDto.getAppointmentTime());
        }

        // Tạo lịch hẹn mới (Appointment)
        Appointment appointment = Appointment.builder()
                .ticket(ticket)
                .technician(technician)
                .appointmentTime(scheduleDto.getAppointmentTime())
                .status("DA_XAC_NHAN")
                .createdAt(LocalDateTime.now())
                .build();
        appointmentRepository.save(appointment);

        // Cập nhật thông tin phân công trên ticket
        String previousStatus = ticket.getStatus();
        ticket.setTechnician(technician);
        ticket.setStatus("DA_PHAN_CONG");
        Ticket savedTicket = ticketRepository.save(ticket);

        // Ghi nhận lịch sử kiểm toán chuyển đổi trạng thái (TicketStatusLog)
        TicketStatusLog statusLog = TicketStatusLog.builder()
                .ticket(savedTicket)
                .fromStatus(previousStatus)
                .toStatus("DA_PHAN_CONG")
                .changedBy(scheduleDto.getScheduledBy())
                .changeReason(String.format("Điều phối kỹ thuật viên %s (Mã NV: %d) lúc %s (QT-07)",
                        technician.getFullName(), technician.getEmployeeId(), scheduleDto.getAppointmentTime()))
                .changedAt(LocalDateTime.now())
                .build();
        statusLogRepository.save(statusLog);

        return TicketResponseDto.builder()
                .ticketId(savedTicket.getTicketId())
                .ticketCode(savedTicket.getTicketCode())
                .customerId(savedTicket.getCustomerId())
                .deviceId(savedTicket.getDevice() != null ? savedTicket.getDevice().getDeviceId() : null)
                .deviceName(savedTicket.getDevice() != null ? savedTicket.getDevice().getDeviceName() : null)
                .serialNo(savedTicket.getDevice() != null ? savedTicket.getDevice().getSerialNo() : null)
                .centerId(savedTicket.getCenterId())
                .technicianId(technician.getTechnicianId())
                .technicianName(technician.getFullName())
                .categoryId(savedTicket.getCategory() != null ? savedTicket.getCategory().getCategoryId() : null)
                .categoryName(savedTicket.getCategory() != null ? savedTicket.getCategory().getCategoryName() : null)
                .issueDesc(savedTicket.getIssueDesc())
                .priority(savedTicket.getPriority())
                .status(savedTicket.getStatus())
                .receivedAt(savedTicket.getReceivedAt())
                .dueDate(savedTicket.getDueDate())
                .closedAt(savedTicket.getClosedAt())
                .build();
    }

    private TechnicianDto mapTechnicianToDto(Technician tech) {
        return TechnicianDto.builder()
                .technicianId(tech.getTechnicianId())
                .employeeId(tech.getEmployeeId())
                .fullName(tech.getFullName())
                .centerId(tech.getCenterId())
                .level(tech.getLevel())
                .isActive(tech.getIsActive())
                .build();
    }
}
