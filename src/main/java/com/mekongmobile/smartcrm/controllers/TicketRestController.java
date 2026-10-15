package com.mekongmobile.smartcrm.controllers;

import com.mekongmobile.smartcrm.dto.ApiResponse;
import com.mekongmobile.smartcrm.dto.TicketRequestDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.dto.UpdateTicketStatusDto;
import com.mekongmobile.smartcrm.services.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TẦNG 1: REST API Controller cho nghiệp vụ quản lý Ticket (QT-04, QT-08).
 */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketRestController {

    private final TicketService ticketService;

    /**
     * QT-04: API tiếp nhận và tạo mới yêu cầu sự cố kỹ thuật.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponseDto>> createTicket(@Valid @RequestBody TicketRequestDto requestDto) {
        TicketResponseDto createdTicket = ticketService.createTicket(requestDto);
        return new ResponseEntity<>(
                ApiResponse.of("Tiếp nhận phiếu yêu cầu thành công (QT-04)", createdTicket),
                HttpStatus.CREATED
        );
    }

    /**
     * API tra cứu chi tiết ticket theo ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponseDto>> getTicketById(@PathVariable("id") Long id) {
        TicketResponseDto ticket = ticketService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.of(ticket));
    }

    /**
     * API lấy danh sách tất cả các ticket.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketResponseDto>>> getAllTickets(
            @RequestParam(value = "status", required = false) String status) {
        List<TicketResponseDto> tickets = (status != null && !status.isBlank())
                ? ticketService.getTicketsByStatus(status)
                : ticketService.getAllTickets();
        return ResponseEntity.ok(ApiResponse.of(tickets));
    }

    /**
     * QT-08: API cập nhật tiến độ và trạng thái ticket.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TicketResponseDto>> updateTicketStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTicketStatusDto updateDto) {
        TicketResponseDto updatedTicket = ticketService.updateTicketStatus(id, updateDto);
        return ResponseEntity.ok(ApiResponse.of("Cập nhật trạng thái ticket thành công (QT-08)", updatedTicket));
    }
}
