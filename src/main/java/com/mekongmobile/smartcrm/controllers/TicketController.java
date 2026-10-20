package com.mekongmobile.smartcrm.controllers;

import com.mekongmobile.smartcrm.dto.AssignTechnicianRequest;
import com.mekongmobile.smartcrm.dto.TechnicianResponseDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.entities.Ticket;
import com.mekongmobile.smartcrm.exceptions.AppException;
import com.mekongmobile.smartcrm.services.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * TẦNG 1: Web Controller quản lý màn hình Phiếu Bảo Hành (Ticket) cho Luồng L4.
 * Tuân thủ mô hình 3 lớp: chỉ tương tác với Service, không gọi Repository.
 */
@Controller
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketService ticketService;

    /**
     * MÀN HÌNH 1: Hiển thị danh sách phiếu bảo hành chưa phân công xếp theo SLA dueDate tăng dần.
     * QT-14: Quản lý chỉ lấy danh sách phiếu thuộc center_id phụ trách.
     */
    @GetMapping("/unassigned")
    public String getUnassignedTickets(Model model, jakarta.servlet.http.HttpSession session) {
        log.info("GET /tickets/unassigned - Tải danh sách phiếu chưa phân công theo SLA");

        com.mekongmobile.smartcrm.dto.UserSessionDto currentUser =
                (com.mekongmobile.smartcrm.dto.UserSessionDto) session.getAttribute("currentUser");
        Long centerId = (currentUser != null) ? currentUser.getCenterId() : null;

        // QT-14: Phân vùng dữ liệu theo trung tâm phụ trách của Quản lý
        List<TicketResponseDto> unassignedTickets = ticketService.getUnassignedTicketsSortedByDueDate(centerId);

        // Thống kê nhanh cho thẻ KPI
        long totalUnassigned = unassignedTickets.size();
        long overdueCount = unassignedTickets.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(java.time.LocalDateTime.now()))
                .count();

        model.addAttribute("tickets", unassignedTickets);
        model.addAttribute("totalUnassigned", totalUnassigned);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("title", "Smart CRM - Danh sách phiếu chưa gán KTV");

        return "tickets/unassigned-list";
    }

    /**
     * MÀN HÌNH 2: Hiển thị form phân công hoặc đổi Kỹ thuật viên (QT-07, QT-08).
     */
    @GetMapping("/{id}/assign")
    public String showAssignForm(@PathVariable("id") Long id, Model model) {
        log.info("GET /tickets/{}/assign - Hiển thị form phân công KTV", id);
        try {
            TicketResponseDto ticket = ticketService.getTicketById(id);
            List<TechnicianResponseDto> eligibleTechnicians = ticketService.getEligibleTechniciansForTicket(id);

            AssignTechnicianRequest request = new AssignTechnicianRequest();
            if (ticket.getTechnicianId() != null) {
                request.setTechnicianId(ticket.getTechnicianId());
            }

            model.addAttribute("ticket", ticket);
            model.addAttribute("technicians", eligibleTechnicians);
            model.addAttribute("assignRequest", request);
            model.addAttribute("title", "Phân công Kỹ thuật viên - Phiếu #" + ticket.getTicketCode());

            return "tickets/assign-form";
        } catch (AppException ex) {
            log.error("Lỗi khi tải form phân công ticket id {}: {}", id, ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            return "redirect:/tickets/unassigned";
        }
    }

    /**
     * Tiếp nhận form submit phân công / đổi Kỹ thuật viên (QT-07, QT-08).
     */
    @PostMapping("/{id}/assign")
    public String assignTechnician(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("assignRequest") AssignTechnicianRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.info("POST /tickets/{}/assign - Tiếp nhận yêu cầu phân công KTV: {}", id, request);

        if (bindingResult.hasErrors()) {
            loadFormContext(id, model);
            return "tickets/assign-form";
        }

        try {
            // Giả lập ID người phân công đăng nhập (Admin/Manager id = 1)
            Long assignedBy = 1L;
            Ticket assignedTicket = ticketService.assignTechnician(
                    id, request.getTechnicianId(), assignedBy, request.getChangeReason());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Phân công kỹ thuật viên " + assignedTicket.getTechnician().getFullName()
                            + " cho phiếu #" + assignedTicket.getTicketCode() + " thành công!");

            return "redirect:/tickets/unassigned";
        } catch (AppException ex) {
            log.warn("Nghiệp vụ từ chối phân công KTV cho ticket {}: {}", id, ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            loadFormContext(id, model);
            return "tickets/assign-form";
        } catch (Exception ex) {
            log.error("Lỗi hệ thống khi phân công KTV cho ticket {}: {}", id, ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi trong quá trình phân công: " + ex.getMessage());
            loadFormContext(id, model);
            return "tickets/assign-form";
        }
    }

    private void loadFormContext(Long ticketId, Model model) {
        try {
            TicketResponseDto ticket = ticketService.getTicketById(ticketId);
            List<TechnicianResponseDto> eligibleTechnicians = ticketService.getEligibleTechniciansForTicket(ticketId);
            model.addAttribute("ticket", ticket);
            model.addAttribute("technicians", eligibleTechnicians);
            model.addAttribute("title", "Phân công Kỹ thuật viên - Phiếu #" + ticket.getTicketCode());
        } catch (Exception ignored) {
        }
    }
}
