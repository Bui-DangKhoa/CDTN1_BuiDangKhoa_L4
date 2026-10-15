package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.dto.ScheduleAppointmentDto;
import com.mekongmobile.smartcrm.dto.TechnicianDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TẦNG 2: Giao diện dịch vụ nghiệp vụ điều phối kỹ thuật viên (QT-07).
 */
public interface SchedulingService {

    /**
     * Tìm kiếm danh sách kỹ thuật viên đủ điều kiện tiếp nhận ticket tại thời điểm hẹn:
     * - Cùng trung tâm bảo hành (centerId)
     * - Có kỹ năng phù hợp với danh mục sự cố của ticket
     * - Đang ở trạng thái hoạt động (isActive = true)
     * - Không bị trùng lịch hẹn với ticket khác tại thời điểm đó
     */
    List<TechnicianDto> findEligibleTechnicians(Long ticketId, LocalDateTime appointmentTime);

    /**
     * QT-07: Đặt lịch hẹn và điều phối kỹ thuật viên cho ticket.
     * Cập nhật technician cho ticket, chuyển trạng thái sang DA_PHAN_CONG, và ghi lịch sử trạng thái.
     */
    TicketResponseDto scheduleAppointment(ScheduleAppointmentDto scheduleDto);
}
