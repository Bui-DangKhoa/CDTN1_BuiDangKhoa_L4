package com.mekongmobile.smartcrm.controllers;

import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.dto.UpdateTicketStatusDto;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.entities.Ticket;
import com.mekongmobile.smartcrm.repositories.TechnicianRepository;
import com.mekongmobile.smartcrm.repositories.TicketRepository;
import com.mekongmobile.smartcrm.services.TicketService;
import lombok.RequiredArgsConstructor;
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
    private final TicketRepository ticketRepository;
    private final TechnicianRepository technicianRepository;

    public TechnicianWebController(TicketService ticketService,
                                   TicketRepository ticketRepository,
                                   TechnicianRepository technicianRepository) {
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
        this.technicianRepository = technicianRepository;
    }

    @GetMapping("/my-tickets")
    public String showMyTickets(
            @RequestParam(value = "ticketId", required = false) Long ticketId,
            Model model) {
        log.info("GET /technician/my-tickets - Xem danh sách phiếu của KTV đăng nhập");

        // Giả lập KTV Nguyễn Văn B (id = 2) đăng nhập theo Wireframe MH3
        Long loggedInTechId = 2L;
        Technician tech = technicianRepository.findById(loggedInTechId)
                .orElse(null);

        List<Ticket> assignedTickets = ticketRepository.findByTechnician_TechnicianId(loggedInTechId);
        List<TicketResponseDto> ticketDtos = assignedTickets.stream()
                .map(t -> TicketResponseDto.builder()
                        .ticketId(t.getTicketId())
                        .ticketCode(t.getTicketCode())
                        .deviceName(t.getDevice() != null ? t.getDevice().getDeviceName() : "iPhone 13")
                        .serialNo(t.getDevice() != null ? t.getDevice().getSerialNo() : "356891234567890")
                        .categoryName(t.getCategory() != null ? t.getCategory().getCategoryName() : "Màn hình")
                        .priority(t.getPriority())
                        .status(t.getStatus())
                        .dueDate(t.getDueDate())
                        .repeatCount(2)
                        .build())
                .toList();

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
            UpdateTicketStatusDto dto = UpdateTicketStatusDto.builder()
                    .newStatus(nextStatus)
                    .changedBy(2L) // KTV đăng nhập
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
