package com.mekongmobile.smartcrm;

import com.mekongmobile.smartcrm.controllers.TechnicianWebController;
import com.mekongmobile.smartcrm.controllers.TicketController;
import com.mekongmobile.smartcrm.dto.TechnicianResponseDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.entities.Ticket;
import com.mekongmobile.smartcrm.repositories.TechnicianRepository;
import com.mekongmobile.smartcrm.repositories.TicketRepository;
import com.mekongmobile.smartcrm.services.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({TicketController.class, TechnicianWebController.class})
public class ThymeleafViewsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketService ticketService;

    @MockBean
    private TicketRepository ticketRepository;

    @MockBean
    private TechnicianRepository technicianRepository;

    @Test
    @DisplayName("MH1: Render /tickets/unassigned thành công, trả về view tickets/unassigned-list")
    void testRenderUnassignedList() throws Exception {
        TicketResponseDto ticket = TicketResponseDto.builder()
                .ticketId(125L)
                .ticketCode("BH-00125")
                .deviceName("iPhone 13")
                .categoryName("Màn hình")
                .priority("CAO")
                .dueDate(LocalDateTime.now().plusHours(12))
                .status("MOI")
                .repeatCount(2)
                .build();

        when(ticketService.getUnassignedTicketsSortedByDueDate()).thenReturn(List.of(ticket));

        mockMvc.perform(get("/tickets/unassigned"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/unassigned-list"))
                .andExpect(model().attributeExists("tickets", "totalUnassigned", "overdueCount"));
    }

    @Test
    @DisplayName("MH2: Render /tickets/{id}/assign thành công, trả về view tickets/assign-form")
    void testRenderAssignForm() throws Exception {
        TicketResponseDto ticket = TicketResponseDto.builder()
                .ticketId(125L)
                .ticketCode("BH-00125")
                .deviceName("iPhone 13")
                .categoryName("Màn hình")
                .priority("CAO")
                .dueDate(LocalDateTime.now().plusHours(12))
                .status("MOI")
                .repeatCount(2)
                .build();

        TechnicianResponseDto tech = TechnicianResponseDto.builder()
                .technicianId(2L)
                .fullName("Nguyễn Văn B")
                .level("4")
                .activeTicketsCount(3)
                .build();

        when(ticketService.getTicketById(125L)).thenReturn(ticket);
        when(ticketService.getEligibleTechniciansForTicket(125L)).thenReturn(List.of(tech));

        mockMvc.perform(get("/tickets/125/assign"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/assign-form"))
                .andExpect(model().attributeExists("ticket", "technicians", "assignRequest"));
    }

    @Test
    @DisplayName("MH3: Render /technician/my-tickets thành công, trả về view technician/my-tickets")
    void testRenderMyTickets() throws Exception {
        Technician tech = Technician.builder()
                .technicianId(2L)
                .employeeId(1002L)
                .fullName("Nguyễn Văn B")
                .centerId(1L)
                .level("4")
                .build();

        com.mekongmobile.smartcrm.entities.Device device = com.mekongmobile.smartcrm.entities.Device.builder()
                .deviceId(1L)
                .customerId(101L)
                .deviceName("iPhone 13")
                .serialNo("356891234567890")
                .build();

        com.mekongmobile.smartcrm.entities.IssueCategory category = com.mekongmobile.smartcrm.entities.IssueCategory.builder()
                .categoryId(1)
                .categoryName("Màn hình")
                .defaultPriority("CAO")
                .build();

        Ticket ticket = Ticket.builder()
                .ticketId(125L)
                .ticketCode("BH-00125")
                .device(device)
                .category(category)
                .status("DANG_XU_LY")
                .priority("CAO")
                .dueDate(LocalDateTime.now().plusHours(12))
                .build();

        when(technicianRepository.findById(anyLong())).thenReturn(Optional.of(tech));
        when(ticketRepository.findByTechnician_TechnicianId(anyLong())).thenReturn(List.of(ticket));

        mockMvc.perform(get("/technician/my-tickets"))
                .andExpect(status().isOk())
                .andExpect(view().name("technician/my-tickets"))
                .andExpect(model().attributeExists("technician", "myTickets"));
    }
}
