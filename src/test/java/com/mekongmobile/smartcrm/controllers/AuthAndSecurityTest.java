package com.mekongmobile.smartcrm.controllers;

import com.mekongmobile.smartcrm.dto.UserSessionDto;
import com.mekongmobile.smartcrm.entities.Appointment;
import com.mekongmobile.smartcrm.entities.Ticket;
import com.mekongmobile.smartcrm.services.AppointmentService;
import com.mekongmobile.smartcrm.services.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private AppointmentService appointmentService;

    @Test
    @DisplayName("TC-SEC-01: Truy cập public /lookup không cần đăng nhập -> Trả về 200 OK")
    void testPublicLookupAccessWithoutLogin() throws Exception {
        mockMvc.perform(get("/lookup"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/appointment-lookup"));
    }

    @Test
    @DisplayName("TC-SEC-01b: Endpoint kiểm tra sức khỏe /actuator/health công khai -> Trả về 200 OK")
    void testActuatorHealthPublicAccess() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("TC-SEC-02: Truy cập internal /tickets/unassigned khi chưa đăng nhập -> Redirect về /login")
    void testUnauthenticatedAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/tickets/unassigned"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("TC-SEC-03: Kỹ thuật viên (KY_THUAT_VIEN) cố truy cập URL Quản lý -> Bị chặn 403 Forbidden")
    void testTechnicianAccessingManagerUrlForbidden() throws Exception {
        UserSessionDto techSession = UserSessionDto.builder()
                .userId(2L)
                .username("ktv1")
                .role("KY_THUAT_VIEN")
                .centerId(1L)
                .build();

        mockMvc.perform(get("/tickets/125/assign").sessionAttr("currentUser", techSession))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-SEC-04: Đăng nhập thành công với vai trò QUAN_LY -> Redirect đến /tickets/unassigned")
    void testLoginSuccessForManager() throws Exception {
        UserSessionDto managerSession = UserSessionDto.builder()
                .userId(1L)
                .username("quanly1")
                .fullName("Nguyễn Quản Lý")
                .role("QUAN_LY")
                .centerId(1L)
                .build();

        when(authService.login(any())).thenReturn(managerSession);

        mockMvc.perform(post("/login")
                        .param("username", "quanly1")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets/unassigned"));
    }

    @Test
    @DisplayName("TC-SEC-05: Đăng nhập thành công với vai trò KY_THUAT_VIEN -> Redirect đến /technician/my-tickets")
    void testLoginSuccessForTechnician() throws Exception {
        UserSessionDto techSession = UserSessionDto.builder()
                .userId(2L)
                .username("ktv1")
                .fullName("Trần Kỹ Thuật")
                .role("KY_THUAT_VIEN")
                .centerId(1L)
                .build();

        when(authService.login(any())).thenReturn(techSession);

        mockMvc.perform(post("/login")
                        .param("username", "ktv1")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/technician/my-tickets"));
    }

    @Test
    @DisplayName("TC-SEC-06: US7/FR7 - Khách hàng xác nhận lịch hẹn tại /lookup/confirm/{id} -> Chuyển DA_XAC_NHAN")
    void testCustomerConfirmAppointment() throws Exception {
        Ticket mockTicket = Ticket.builder()
                .ticketId(10L)
                .ticketCode("BH-00125")
                .build();

        Appointment confirmedAppointment = Appointment.builder()
                .appointmentId(99L)
                .ticket(mockTicket)
                .status("DA_XAC_NHAN")
                .appointmentTime(LocalDateTime.now().plusDays(1))
                .build();

        when(appointmentService.confirmAppointment(eq(99L))).thenReturn(confirmedAppointment);

        mockMvc.perform(post("/lookup/confirm/99").param("keyword", "BH-00125"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lookup?keyword=BH-00125"))
                .andExpect(flash().attributeExists("successMessage"));
    }
    @Test
    @DisplayName("TC-SEC-07: QT-15 - Tra cứu tại /lookup hiển thị SĐT đã che (090****567) và không chứa SĐT gốc (0901234567)")
    void testLookupCustomerPhoneMasked_QT15() throws Exception {
        com.mekongmobile.smartcrm.dto.AppointmentResponseDto dto = com.mekongmobile.smartcrm.dto.AppointmentResponseDto.builder()
                .appointmentId(100L)
                .ticketCode("BH-00125")
                .customerPhone("090****567")
                .technicianName("Nguyễn Văn Kỹ Thuật")
                .deviceName("iPhone 13")
                .issueDesc("Lỗi màn hình cảm ứng")
                .appointmentTime(LocalDateTime.now().plusDays(1))
                .status("CHO_XAC_NHAN")
                .build();

        when(appointmentService.findAppointmentsByKeyword("BH-00125")).thenReturn(java.util.List.of(dto));

        mockMvc.perform(get("/lookup").param("keyword", "BH-00125"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/appointment-lookup"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("090****567")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("0901234567"))));
    }
}
