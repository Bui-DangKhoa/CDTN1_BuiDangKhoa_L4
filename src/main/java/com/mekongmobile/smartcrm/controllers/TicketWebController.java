package com.mekongmobile.smartcrm.controllers;

import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.services.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * TẦNG 1: Web UI Controller (Sử dụng Spring MVC & Thymeleaf)
 * Phục vụ người dùng quản trị và nhân viên điều phối Mekong Mobile.
 */
@Controller
@Slf4j
public class TicketWebController {

    private final TicketService ticketService;

    public TicketWebController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * Đường dẫn gốc (/) mặc định chuyển hướng sang Màn hình 1 (MH1):
     * Danh sách phiếu chưa phân công theo Wireframe và Bảng đối chiếu 5.2.
     */
    @GetMapping("/")
    public String index() {
        log.info("GET / - Chuyển hướng tới Màn hình 1 (/tickets/unassigned) theo Wireframe");
        return "redirect:/tickets/unassigned";
    }

    @GetMapping("/tickets/{id}")
    public String ticketDetail(@PathVariable("id") Long id, Model model) {
        TicketResponseDto ticket = ticketService.getTicketById(id);
        model.addAttribute("ticket", ticket);
        model.addAttribute("title", "Chi tiết Ticket - " + ticket.getTicketCode());
        return "redirect:/tickets/" + id + "/assign";
    }
}
