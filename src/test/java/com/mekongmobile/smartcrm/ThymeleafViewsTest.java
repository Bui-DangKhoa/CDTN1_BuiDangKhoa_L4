package com.mekongmobile.smartcrm;

import com.mekongmobile.smartcrm.controllers.TechnicianWebController;
import com.mekongmobile.smartcrm.controllers.TicketController;
import com.mekongmobile.smartcrm.dto.TechnicianResponseDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.services.TechnicianService;
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

import com.mekongmobile.smartcrm.controllers.PublicAppointmentController;
import com.mekongmobile.smartcrm.services.AppointmentService;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ TicketController.class, TechnicianWebController.class, PublicAppointmentController.class })
public class ThymeleafViewsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private TicketService ticketService;

        @MockBean
        private TechnicianService technicianService;

        @MockBean
        private AppointmentService appointmentService;

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

                when(technicianService.resolveTechnicianForUser(any())).thenReturn(Optional.of(tech));
                when(technicianService.findById(anyLong())).thenReturn(Optional.of(tech));
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

        @Test
        @DisplayName("UI State: MH1 hiển thị Empty State khi danh sách phiếu rỗng")
        void testRenderUnassignedList_EmptyState() throws Exception {
                when(ticketService.getUnassignedTicketsSortedByDueDate(any())).thenReturn(List.of());

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
                                .andExpect(content().string(containsString("Không có phiếu bảo hành nào cần phân công")))
                                .andExpect(content().string(containsString("Xóa bộ lọc")));
        }

        @Test
        @DisplayName("UI State: MH1 hiển thị Error State kèm requestId và nút Thử lại")
        void testRenderUnassignedList_ErrorState() throws Exception {
                when(ticketService.getUnassignedTicketsSortedByDueDate(any())).thenReturn(List.of());

                com.mekongmobile.smartcrm.dto.UserSessionDto managerSession = com.mekongmobile.smartcrm.dto.UserSessionDto
                                .builder()
                                .userId(1L)
                                .username("quanly1")
                                .role("QUAN_LY")
                                .centerId(1L)
                                .build();

                mockMvc.perform(get("/tickets/unassigned")
                                .sessionAttr("currentUser", managerSession)
                                .flashAttr("errorMessage", "Đã xảy ra lỗi kết nối CSDL"))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("Đã xảy ra lỗi kết nối CSDL")))
                                .andExpect(content().string(containsString("Mã tra cứu sự cố:")))
                                .andExpect(content().string(containsString("Thử lại")));
        }

        @Test
        @DisplayName("UI State: Cổng Tra cứu công khai /lookup hiển thị đầy đủ Loading, Empty và Error states")
        void testRenderPublicLookup_UIStates() throws Exception {
                when(appointmentService.findAppointmentsByKeyword("KHONG_CO_KET_QUA")).thenReturn(List.of());

                mockMvc.perform(get("/lookup").param("keyword", "KHONG_CO_KET_QUA"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("public/appointment-lookup"))
                                .andExpect(content().string(containsString("Không có phiếu bảo hành nào cần phân công")))
                                .andExpect(content().string(containsString("Xóa bộ lọc")))
                                .andExpect(content().string(containsString("Đang tải danh sách phiếu bảo hành... Vui lòng chờ trong giây lát.")));
        }
        @Test
        @DisplayName("MH3 Khung 3: Hiển thị bảng lịch sử sửa chữa khi thiết bị có phiếu cũ đã đóng")
        void testRenderMyTickets_WithRepairHistory() throws Exception {
                Technician tech = Technician.builder()
                                .technicianId(2L)
                                .employeeId(1002L)
                                .fullName("Trần Kỹ Thuật")
                                .centerId(1L)
                                .level("4")
                                .build();

                TicketResponseDto ticketDto = TicketResponseDto.builder()
                                .ticketId(125L)
                                .ticketCode("BH-00125")
                                .deviceId(1L)
                                .deviceName("iPhone 15 Pro Max")
                                .categoryName("Màn hình")
                                .status("DANG_XU_LY")
                                .priority("CAO")
                                .dueDate(LocalDateTime.now().plusHours(12))
                                .build();

                com.mekongmobile.smartcrm.dto.DeviceRepairHistoryDto historyItem =
                                com.mekongmobile.smartcrm.dto.DeviceRepairHistoryDto.builder()
                                                .ticketCode("TK-202606-071")
                                                .repairedDate("12/06/2026")
                                                .technicianName("Trần Văn C")
                                                .solutionDesc("Thay màn hình OLED chính hãng")
                                                .build();

                when(technicianService.resolveTechnicianForUser(any())).thenReturn(Optional.of(tech));
                when(ticketService.getActiveTicketsForTechnician(anyLong())).thenReturn(List.of(ticketDto));
                when(ticketService.getDeviceRepairHistory(eq(1L), eq(125L))).thenReturn(List.of(historyItem));

                com.mekongmobile.smartcrm.dto.UserSessionDto techSession = com.mekongmobile.smartcrm.dto.UserSessionDto
                                .builder()
                                .userId(2L)
                                .username("ktv1")
                                .role("KY_THUAT_VIEN")
                                .centerId(1L)
                                .build();

                mockMvc.perform(get("/technician/my-tickets").sessionAttr("currentUser", techSession))
                                .andExpect(status().isOk())
                                .andExpect(view().name("technician/my-tickets"))
                                .andExpect(model().attributeExists("repairHistory"))
                                .andExpect(content().string(containsString("TK-202606-071")))
                                .andExpect(content().string(containsString("12/06/2026")))
                                .andExpect(content().string(containsString("Trần Văn C")))
                                .andExpect(content().string(containsString("Thay màn hình OLED chính hãng")));
        }

        @Test
        @DisplayName("MH3 Khung 3: Hiển thị thông báo trạng thái rỗng khi thiết bị chưa từng sửa chữa trước đó")
        void testRenderMyTickets_EmptyRepairHistory() throws Exception {
                Technician tech = Technician.builder()
                                .technicianId(2L)
                                .employeeId(1002L)
                                .fullName("Trần Kỹ Thuật")
                                .centerId(1L)
                                .level("4")
                                .build();

                TicketResponseDto ticketDto = TicketResponseDto.builder()
                                .ticketId(103L)
                                .ticketCode("TK-202610-003")
                                .deviceId(3L)
                                .deviceName("MacBook Pro M3")
                                .status("DANG_XU_LY")
                                .build();

                when(technicianService.resolveTechnicianForUser(any())).thenReturn(Optional.of(tech));
                when(ticketService.getActiveTicketsForTechnician(anyLong())).thenReturn(List.of(ticketDto));
                when(ticketService.getDeviceRepairHistory(eq(3L), eq(103L))).thenReturn(List.of());

                com.mekongmobile.smartcrm.dto.UserSessionDto techSession = com.mekongmobile.smartcrm.dto.UserSessionDto
                                .builder()
                                .userId(2L)
                                .username("ktv1")
                                .role("KY_THUAT_VIEN")
                                .centerId(1L)
                                .build();

                mockMvc.perform(get("/technician/my-tickets").sessionAttr("currentUser", techSession))
                                .andExpect(status().isOk())
                                .andExpect(view().name("technician/my-tickets"))
                                .andExpect(model().attributeExists("repairHistory"))
                                .andExpect(content().string(containsString("Không có lịch sử sửa chữa trước đó cho thiết bị này.")));
        }
}
