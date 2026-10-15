package com.mekongmobile.smartcrm.controllers;

import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.services.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * TẦNG 1: Web UI Controller (Sử dụng Spring MVC & Thymeleaf)
 * Phục vụ người dùng quản trị và nhân viên điều phối Mekong Mobile.
 */
@Controller
@RequiredArgsConstructor
public class TicketWebController {

    private final TicketService ticketService;

    @GetMapping("/")
    public String index(Model model) {
        List<TicketResponseDto> tickets = ticketService.getAllTickets();
        model.addAttribute("tickets", tickets);
        model.addAttribute("title", "Smart CRM - Bảng điều khiển điều phối kỹ thuật viên");
        return "index";
    }

    @GetMapping("/tickets/{id}")
    public String ticketDetail(@PathVariable("id") Long id, Model model) {
        TicketResponseDto ticket = ticketService.getTicketById(id);
        model.addAttribute("ticket", ticket);
        model.addAttribute("title", "Chi tiết Ticket - " + ticket.getTicketCode());
        return "index";
    }
}
