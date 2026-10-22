package com.mekongmobile.smartcrm.controllers;

import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.dto.UpdateTicketStatusDto;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.repositories.TechnicianRepository;
import com.mekongmobile.smartcrm.services.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * TẦNG 1: Controller phục vụ Màn hình Kỹ thuật viên cá nhân (Màn hình 3 / MH3).
 * Đáp ứng US4, US5, US6 (FR4, FR5, FR6 - Quy tắc QT-06).
 */
@Controller
@RequestMapping("/technician")
@Slf4j
public class TechnicianWebController {

    private final TicketService ticketService;
    private final TechnicianRepository technicianRepository;

    public TechnicianWebController(TicketService ticketService,
                                   TechnicianRepository technicianRepository) {
        this.ticketService = ticketService;
        this.technicianRepository = technicianRepository;
    }

    @GetMapping("/my-tickets")
    public String showMyTickets(
            @RequestParam(value = "ticketId", required = false) Long ticketId,
            jakarta.servlet.http.HttpSession session,
            Model model) {
        log.info("GET /technician/my-tickets - Xem danh sách phiếu của KTV đăng nhập");

        com.mekongmobile.smartcrm.dto.UserSessionDto currentUser =
                (com.mekongmobile.smartcrm.dto.UserSessionDto) session.getAttribute("currentUser");

        // Tìm kiếm Kỹ thuật viên tương ứng: ưu tiên theo họ tên tài khoản đăng nhập (vd: Trần Kỹ Thuật), sau đó theo ID
        Technician tech = null;
        if (currentUser != null && currentUser.getFullName() != null) {
            tech = technicianRepository.findByFullName(currentUser.getFullName()).orElse(null);
        }
        if (tech == null && currentUser != null && currentUser.getUserId() != null) {
            tech = technicianRepository.findById(currentUser.getUserId()).orElse(null);
        }
        if (tech == null) {
            tech = technicianRepository.findById(1L).orElse(null);
        }

        Long loggedInTechId = (tech != null) ? tech.getTechnicianId() : 1L;

        // QT-15: KTV chỉ xem danh sách phiếu gán cho cá nhân, che 4 số giữa SĐT khách hàng qua DTO Mapper
        List<TicketResponseDto> ticketDtos = ticketService.getTicketsForTechnician(loggedInTechId);

        TicketResponseDto selectedTicket = null;
        if (ticketId != null) {
            selectedTicket = ticketDtos.stream()
                    .filter(t -> t.getTicketId().equals(ticketId))
                    .findFirst()
                    .orElse(null);
        } else if (!ticketDtos.isEmpty()) {
            selectedTicket = ticketDtos.get(0);
        }

        model.addAttribute("technician", tech);
        model.addAttribute("myTickets", ticketDtos);
        model.addAttribute("selectedTicket", selectedTicket);
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

        // Quy tắc kiểm tra QT-06: Chuyển đổi trạng thái hợp lệ
        if ("DANG_XU_LY".equalsIgnoreCase(nextStatus)) {
            // Mô phỏng / Kiểm tra vi phạm QT-06 khi từ trạng thái hoàn tất quay về
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không thể chuyển trạng thái từ HOÀN TẤT về ĐANG XỬ LÝ (Quy tắc QT-06).");
            return "redirect:/technician/my-tickets?ticketId=" + id;
        }

        try {
            com.mekongmobile.smartcrm.dto.UserSessionDto currentUser =
                    (com.mekongmobile.smartcrm.dto.UserSessionDto) session.getAttribute("currentUser");

            Technician tech = null;
            if (currentUser != null && currentUser.getFullName() != null) {
                tech = technicianRepository.findByFullName(currentUser.getFullName()).orElse(null);
            }
            Long changedById = (tech != null) ? tech.getTechnicianId() : ((currentUser != null) ? currentUser.getUserId() : 1L);

            UpdateTicketStatusDto dto = UpdateTicketStatusDto.builder()
                    .newStatus(nextStatus)
                    .changedBy(changedById) // KTV đăng nhập
                    .changeReason("Cập nhật tiến độ theo quy trình QT-06")
                    .build();
            ticketService.updateTicketStatus(id, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái phiếu thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/technician/my-tickets?ticketId=" + id;
    }
}
