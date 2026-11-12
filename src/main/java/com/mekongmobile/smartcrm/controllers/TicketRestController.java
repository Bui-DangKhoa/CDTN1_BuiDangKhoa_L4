package com.mekongmobile.smartcrm.controllers;

import com.mekongmobile.smartcrm.dto.ApiResponse;
import com.mekongmobile.smartcrm.dto.TicketRequestDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.dto.UpdateTicketRequestDto;
import com.mekongmobile.smartcrm.dto.UpdateTicketStatusDto;
import com.mekongmobile.smartcrm.dto.UserSessionDto;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.exceptions.AppException;
import com.mekongmobile.smartcrm.services.TicketService;
import com.mekongmobile.smartcrm.services.TechnicianService;
import jakarta.servlet.http.HttpSession;
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
    private final TechnicianService technicianService;

    /**
     * QT-04: API tiếp nhận và tạo mới yêu cầu sự cố kỹ thuật.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponseDto>> createTicket(@Valid @RequestBody TicketRequestDto requestDto,
                                                                        HttpSession session) {
        requireManager(session);
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
    public ResponseEntity<ApiResponse<TicketResponseDto>> getTicketById(@PathVariable("id") Long id,
                                                                         HttpSession session) {
        UserSessionDto user = requireUser(session);
        TicketResponseDto ticket = isTechnician(user)
                ? ticketService.getTicketForTechnician(id, requireTechnician(user).getTechnicianId())
                : ticketService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.of(ticket));
    }

    /**
     * API lấy danh sách tất cả các ticket.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketResponseDto>>> getAllTickets(
            @RequestParam(value = "status", required = false) String status, HttpSession session) {
        UserSessionDto user = requireUser(session);
        List<TicketResponseDto> tickets;
        if (isTechnician(user)) {
            tickets = ticketService.getTicketsForTechnician(requireTechnician(user).getTechnicianId());
            if (status != null && !status.isBlank()) {
                tickets = tickets.stream().filter(ticket -> status.equalsIgnoreCase(ticket.getStatus())).toList();
            }
        } else {
            tickets = (status != null && !status.isBlank())
                    ? ticketService.getTicketsByStatus(status)
                    : ticketService.getAllTickets();
        }
        return ResponseEntity.ok(ApiResponse.of(tickets));
    }

    /**
     * API lấy danh sách phiếu chưa phân công xếp theo SLA dueDate tăng dần.
     */
    @GetMapping("/unassigned")
    public ResponseEntity<ApiResponse<List<TicketResponseDto>>> getUnassignedTickets(HttpSession session) {
        UserSessionDto user = requireUser(session);
        List<TicketResponseDto> tickets = isTechnician(user)
                ? ticketService.getTicketsForTechnician(requireTechnician(user).getTechnicianId())
                : ticketService.getUnassignedTicketsSortedByDueDate();
        return ResponseEntity.ok(ApiResponse.of("Danh sách phiếu chưa phân công theo SLA", tickets));
    }

    /**
     * QT-07 & QT-08: API phân công hoặc đổi kỹ thuật viên cho ticket.
     */
    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<TicketResponseDto>> assignTechnician(
            @PathVariable("id") Long id,
            @Valid @RequestBody com.mekongmobile.smartcrm.dto.AssignTechnicianRequest request,
            HttpSession session) {
        Long assignedBy = requireManager(session).getUserId();
        com.mekongmobile.smartcrm.entities.Ticket ticket = ticketService.assignTechnician(
                id, request.getTechnicianId(), assignedBy, request.getChangeReason());
        TicketResponseDto responseDto = ticketService.getTicketById(ticket.getTicketId());
        return ResponseEntity.ok(ApiResponse.of("Phân công kỹ thuật viên thành công (QT-07, QT-08)", responseDto));
    }

    /**
     * QT-08: API cập nhật tiến độ và trạng thái ticket.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TicketResponseDto>> updateTicketStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTicketStatusDto updateDto,
            HttpSession session) {
        UserSessionDto user = requireUser(session);
        Long technicianId = isTechnician(user) ? requireTechnician(user).getTechnicianId() : null;
        if (technicianId != null) {
            TicketResponseDto current = ticketService.getTicketById(id);
            if (!technicianId.equals(current.getTechnicianId())) {
                throw new AppException("FORBIDDEN_NOT_OWNER", "Phiếu này không thuộc kỹ thuật viên đang đăng nhập");
            }
        }
        updateDto.setChangedBy(user.getUserId());
        TicketResponseDto updatedTicket = ticketService.updateTicketStatus(id, updateDto, user.getUserId(), technicianId);
        return ResponseEntity.ok(ApiResponse.of("Cập nhật trạng thái ticket thành công (QT-06)", updatedTicket));
    }

    /**
     * API cập nhật thông tin phiếu bảo hành (issueDesc, priority, categoryId, customerPhone).
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponseDto>> updateTicket(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTicketRequestDto requestDto,
            HttpSession session) {
        requireManager(session);
        TicketResponseDto updated = ticketService.updateTicket(id, requestDto);
        return ResponseEntity.ok(ApiResponse.of("Cập nhật thông tin phiếu thành công", updated));
    }

    /**
     * QT-13: API xóa mềm phiếu bảo hành.
     * Người thao tác được lấy từ người dùng đăng nhập (session).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(
            @PathVariable("id") Long id,
            HttpSession session) {
        Long changedBy = requireManager(session).getUserId();
        ticketService.deleteTicket(id, changedBy);
        return ResponseEntity.ok(ApiResponse.of("Xóa mềm phiếu bảo hành thành công (QT-13)", null));
    }

    private UserSessionDto requireUser(HttpSession session) {
        UserSessionDto user = session != null ? (UserSessionDto) session.getAttribute("currentUser") : null;
        if (user == null || user.getUserId() == null) {
            throw new AppException("UNAUTHORIZED", "Vui lòng đăng nhập");
        }
        return user;
    }

    private UserSessionDto requireManager(HttpSession session) {
        UserSessionDto user = requireUser(session);
        if (!"QUAN_LY".equalsIgnoreCase(user.getRole())) {
            throw new AppException("FORBIDDEN", "Chức năng này chỉ dành cho quản lý");
        }
        return user;
    }

    private boolean isTechnician(UserSessionDto user) {
        if ("KY_THUAT_VIEN".equalsIgnoreCase(user.getRole())) return true;
        if (!"QUAN_LY".equalsIgnoreCase(user.getRole())) {
            throw new AppException("FORBIDDEN", "Vai trò không được phép truy cập API ticket");
        }
        return false;
    }

    private Technician requireTechnician(UserSessionDto user) {
        return technicianService.resolveTechnicianForUser(user)
                .orElseThrow(() -> new AppException("FORBIDDEN", "Tài khoản chưa được liên kết với hồ sơ kỹ thuật viên"));
    }
}
