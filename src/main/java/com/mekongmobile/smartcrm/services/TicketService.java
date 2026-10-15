package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.dto.TicketRequestDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.dto.UpdateTicketStatusDto;

import java.util.List;

/**
 * TẦNG 2: Giao diện dịch vụ nghiệp vụ xử lý Ticket.
 * Quản lý quy trình QT-04 (Tiếp nhận & Phân loại sự cố) và QT-08 (Cập nhật tiến độ ticket).
 */
public interface TicketService {

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
     * QT-08: Cập nhật trạng thái tiến độ ticket và ghi nhật ký kiểm toán.
     */
    TicketResponseDto updateTicketStatus(Long ticketId, UpdateTicketStatusDto updateDto);
}
