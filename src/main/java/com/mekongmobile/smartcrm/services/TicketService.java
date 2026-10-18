package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.dto.TicketRequestDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.dto.UpdateTicketStatusDto;

import com.mekongmobile.smartcrm.entities.Ticket;
import com.mekongmobile.smartcrm.entities.TicketStatusLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TẦNG 2: Giao diện dịch vụ nghiệp vụ xử lý Ticket.
 * Quản lý quy trình QT-04 (Tiếp nhận & Phân loại sự cố) và QT-08 (Cập nhật tiến độ ticket).
 */
public interface TicketService {

    /**
     * QT-04: Tự động tính hạn xử lý (due_date) theo mức độ ưu tiên:
     * - CAO: +24 giờ
     * - TRUNG_BINH: +72 giờ
     * - THAP: +120 giờ
     */
    LocalDateTime calculateDueDate(LocalDateTime receivedAt, Priority priority);

    /**
     * Overload tính due_date theo String priority.
     */
    LocalDateTime calculateDueDate(LocalDateTime receivedAt, String priority);

    /**
     * QT-06: Ghi nhật ký chuyển đổi trạng thái ticket (TicketStatusLog).
     */
    TicketStatusLog logStatusChange(Ticket ticket, String fromStatus, String toStatus, Long changedBy, String reason);

    /**
     * QT-04: Tiếp nhận và tự động phân loại sự cố kỹ thuật.
     */
    TicketResponseDto createTicket(TicketRequestDto requestDto);

    /**
     * Lấy thông tin chi tiết ticket theo ID.
     */
    TicketResponseDto getTicketById(Long ticketId);

    /**
     * Lấy danh sách tất cả các ticket mới nhất.
     */
    List<TicketResponseDto> getAllTickets();

    /**
     * Lấy danh sách ticket theo trạng thái.
     */
    List<TicketResponseDto> getTicketsByStatus(String status);

    /**
     * QT-08 & QT-07: Phân công hoặc thay đổi kỹ thuật viên phụ trách ticket:
     * - QT-08: KTV phải cùng centerId và có tay nghề (proficiency >= 3) thuộc categoryId của ticket.
     * - QT-07: Bắt buộc lý do đổi KTV (>= 10 ký tự) nếu thay đổi KTV đã gán.
     * - QT-06: Chuyển trạng thái sang DA_PHAN_CONG và ghi nhật ký status log.
     */
    Ticket assignTechnician(Long ticketId, Long technicianId, Long assignedBy, String changeReason);

    /**
     * QT-08: Cập nhật trạng thái tiến độ ticket và ghi nhật ký kiểm toán.
     */
    TicketResponseDto updateTicketStatus(Long ticketId, UpdateTicketStatusDto updateDto);

    /**
     * Lấy danh sách phiếu bảo hành chưa phân công, sắp xếp theo hạn SLA (due_date) tăng dần.
     */
    List<TicketResponseDto> getUnassignedTicketsSortedByDueDate();

    /**
     * QT-08: Lấy danh sách KTV đủ điều kiện (cùng centerId, proficiency >= 3 cho danh mục sự cố).
     */
    List<com.mekongmobile.smartcrm.dto.TechnicianResponseDto> getEligibleTechniciansForTicket(Long ticketId);
}
