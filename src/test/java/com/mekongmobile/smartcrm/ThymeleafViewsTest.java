package com.mekongmobile.smartcrm;

import com.mekongmobile.smartcrm.controllers.TechnicianWebController;
import com.mekongmobile.smartcrm.controllers.TicketController;
import com.mekongmobile.smartcrm.dto.TechnicianResponseDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.repositories.TechnicianRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ TicketController.class, TechnicianWebController.class })
public class ThymeleafViewsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private TicketService ticketService;

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

                when(ticketService.getUnassignedTicketsSortedByDueDate(any())).thenReturn(List.of(ticket));

                com.mekongmobile.smartcrm.dto.UserSessionDto managerSession = com.mekongmobile.smartcrm.dto.UserSessionDto
                                .builder()
                                .userId(1L)
                                .username("quanly1")
                                .fullName("Nguyễn Quản Lý")
                                .role("QUAN_LY")
                                .centerId(1L)
                                .build();

                mockMvc.perform(get("/tickets/unassigned").sessionAttr("currentUser", managerSession))
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

                com.mekongmobile.smartcrm.dto.UserSessionDto managerSession = com.mekongmobile.smartcrm.dto.UserSessionDto
                                .builder()
                                .userId(1L)
                                .username("quanly1")
                                .fullName("Nguyễn Quản Lý")
                                .role("QUAN_LY")
                                .centerId(1L)
                                .build();

                mockMvc.perform(get("/tickets/125/assign").sessionAttr("currentUser", managerSession))
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

                TicketResponseDto ticketDto = TicketResponseDto.builder()
                                .ticketId(125L)
                                .ticketCode("BH-00125")
                                .deviceName("iPhone 13")
                                .customerPhone("090****567")
                                .categoryName("Màn hình")
                                .status("DANG_XU_LY")
                                .priority("CAO")
                                .dueDate(LocalDateTime.now().plusHours(12))
                                .build();

                when(technicianRepository.findById(anyLong())).thenReturn(Optional.of(tech));
                when(ticketService.getTicketsForTechnician(anyLong())).thenReturn(List.of(ticketDto));

                com.mekongmobile.smartcrm.dto.UserSessionDto techSession = com.mekongmobile.smartcrm.dto.UserSessionDto
                                .builder()
                                .userId(2L)
                                .username("ktv1")
                                .fullName("Nguyễn Văn B")
                                .role("KY_THUAT_VIEN")
                                .centerId(1L)
                                .build();

                mockMvc.perform(get("/technician/my-tickets").sessionAttr("currentUser", techSession))
                                .andExpect(status().isOk())
                                .andExpect(view().name("technician/my-tickets"))
                                .andExpect(model().attributeExists("technician", "myTickets"));
        }
}
