package com.mekongmobile.smartcrm.controllers;

import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.dto.UpdateTicketStatusDto;
import com.mekongmobile.smartcrm.dto.UserSessionDto;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.services.TechnicianService;
import com.mekongmobile.smartcrm.services.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * TẦNG 1: Controller phục vụ Màn hình Kỹ thuật viên cá nhân (Màn hình 3 / MH3).
 * Đáp ứng US4, US5, US6 (FR4, FR5, FR6 - Quy tắc QT-06).
 * Tuân thủ mô hình 3 lớp (C2, S2): Chỉ tương tác với Service, tuyệt đối không inject Repository.
 */
@Controller
@RequestMapping("/technician")
@RequiredArgsConstructor
@Slf4j
public class TechnicianWebController {

    private final TicketService ticketService;
    private final TechnicianService technicianService;

    @GetMapping("/my-tickets")
    public String showMyTickets(
            @RequestParam(value = "ticketId", required = false) Long ticketId,
            jakarta.servlet.http.HttpSession session,
            Model model) {
        log.info("GET /technician/my-tickets - Xem danh sách phiếu của KTV đăng nhập");

        UserSessionDto currentUser = (UserSessionDto) session.getAttribute("currentUser");

        // Xác định KTV đăng nhập qua user_id
        Optional<Technician> techOpt = technicianService.resolveTechnicianForUser(currentUser);
        if (techOpt.isEmpty()) {
            log.warn("Tài khoản '{}' (userId: {}) chưa liên kết với kỹ thuật viên nào",
                    currentUser != null ? currentUser.getUsername() : "null",
                    currentUser != null ? currentUser.getUserId() : "null");
            model.addAttribute("technician", null);
            model.addAttribute("myTickets", List.of());
            model.addAttribute("selectedTicket", null);
            model.addAttribute("repairHistory", List.of());
            model.addAttribute("errorMessage", "Tài khoản chưa liên kết với kỹ thuật viên");
            model.addAttribute("title", "Mekong Mobile Smart CRM - Màn hình Kỹ thuật viên");
            return "technician/my-tickets";
        }

        Technician tech = techOpt.get();
        Long loggedInTechId = tech.getTechnicianId();

        // QT-15: KTV chỉ xem danh sách phiếu gán cho cá nhân (DA_PHAN_CONG, DANG_XU_LY), che SĐT khách hàng
        List<TicketResponseDto> ticketDtos = ticketService.getActiveTicketsForTechnician(loggedInTechId);
        if (ticketDtos.isEmpty()) {
            ticketDtos = ticketService.getTicketsForTechnician(loggedInTechId);
        }

        TicketResponseDto selectedTicket = null;
        if (ticketId != null) {
            selectedTicket = ticketDtos.stream()
                    .filter(t -> t.getTicketId().equals(ticketId))
                    .findFirst()
                    .orElse(null);
        } else if (!ticketDtos.isEmpty()) {
            selectedTicket = ticketDtos.get(0);
        }

                List<com.mekongmobile.smartcrm.dto.DeviceRepairHistoryDto> repairHistory = List.of();
        if (selectedTicket != null && selectedTicket.getDeviceId() != null) {
            repairHistory = ticketService.getDeviceRepairHistory(
                    selectedTicket.getDeviceId(), selectedTicket.getTicketId());
        }

        model.addAttribute("technician", tech);
        model.addAttribute("myTickets", ticketDtos);
        model.addAttribute("selectedTicket", selectedTicket);
        model.addAttribute("repairHistory", repairHistory);
        model.addAttribute("title", "Mekong Mobile Smart CRM - Màn hình Kỹ thuật viên");

        return "technician/my-tickets";
    }

    @PostMapping("/tickets/{id}/status")
    public String updateTicketStatus(
            @PathVariable("id") Long id,
            @RequestParam("nextStatus") String nextStatus,
            jakarta.servlet.http.HttpSession session,
            RedirectAttributes redirectAttributes) {
        log.info("POST /technician/tickets/{}/status - Cập nhật trạng thái sang {}", id, nextStatus);

        try {
            UserSessionDto currentUser = (UserSessionDto) session.getAttribute("currentUser");
            Optional<Technician> techOpt = technicianService.resolveTechnicianForUser(currentUser);
            if (techOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản chưa liên kết với kỹ thuật viên");
                return "redirect:/technician/my-tickets";
            }

            Long changedById = techOpt.get().getTechnicianId();

            UpdateTicketStatusDto dto = UpdateTicketStatusDto.builder()
                    .newStatus(nextStatus)
                    .changedBy(changedById) // KTV đăng nhập
                    .changeReason("KTV cập nhật tiến độ theo quy trình QT-06")
                    .build();
            ticketService.updateTicketStatus(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái phiếu thành công!");
        } catch (Exception ex) {
            log.error("Lỗi cập nhật trạng thái phiếu id {}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật trạng thái: " + ex.getMessage());
        }

        return "redirect:/technician/my-tickets";
    }
}
