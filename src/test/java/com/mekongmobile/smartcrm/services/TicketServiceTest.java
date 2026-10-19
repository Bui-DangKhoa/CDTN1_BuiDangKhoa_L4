package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.entities.Device;
import com.mekongmobile.smartcrm.entities.IssueCategory;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.entities.Ticket;
import com.mekongmobile.smartcrm.entities.TicketStatusLog;
import com.mekongmobile.smartcrm.exceptions.AppException;
import com.mekongmobile.smartcrm.repositories.DeviceRepository;
import com.mekongmobile.smartcrm.repositories.IssueCategoryRepository;
import com.mekongmobile.smartcrm.repositories.TechnicianRepository;
import com.mekongmobile.smartcrm.repositories.TicketRepository;
import com.mekongmobile.smartcrm.repositories.TicketStatusLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Bộ kiểm thử tự động Unit Test cho Tầng Service (TicketService).
 * Sử dụng JUnit 5 và Mockito độc lập với cơ sở dữ liệu.
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TechnicianRepository technicianRepository;

    @Mock
    private TicketStatusLogRepository statusLogRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private IssueCategoryRepository categoryRepository;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private Device mockDevice;
    private IssueCategory mockCategory;
    private Ticket mockTicket;
    private Technician mockTechnician;

    @BeforeEach
    void setUp() {
        mockDevice = Device.builder()
                .deviceId(1L)
                .customerId(101L)
                .deviceName("iPhone 15 Pro Max")
                .serialNo("SER-9988")
                .build();

        mockCategory = IssueCategory.builder()
                .categoryId(1)
                .categoryName("Lỗi Nguồn / Sạc")
                .defaultPriority("CAO")
                .build();

        mockTicket = Ticket.builder()
                .ticketId(10L)
                .ticketCode("TCK-2026-001")
                .customerId(101L)
                .device(mockDevice)
                .category(mockCategory)
                .centerId(1L)
                .issueDesc("Máy không sạc được pin")
                .priority("CAO")
                .status("MOI")
                .technician(null)
                .receivedAt(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusHours(24))
                .build();

        mockTechnician = Technician.builder()
                .technicianId(5L)
                .fullName("Nguyễn Văn Kỹ Thuật")
                .centerId(1L)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("TC-01: Happy Path - Phân công KTV đủ tay nghề thành công lần đầu cho phiếu ở trạng thái MOI")
    void TC01_assignTechnician_success_whenValidData() {
        // Given: ticket ở trạng thái MOI, chưa có KTV; KTV cùng trung tâm và đủ tay nghề (proficiency >= 3)
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        when(technicianRepository.findById(5L)).thenReturn(Optional.of(mockTechnician));
        when(technicianRepository.findEligibleTechnicians(1L, 1)).thenReturn(List.of(mockTechnician));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: Gọi phương thức phân công KTV
        Ticket result = ticketService.assignTechnician(10L, 5L, 101L, "Phân công KTV xử lý sự cố nguồn");

        // Then: Phiếu chuyển sang DA_PHAN_CONG, gán đúng KTV và logStatusChange được gọi 1 lần
        assertNotNull(result);
        assertEquals("DA_PHAN_CONG", result.getStatus());
        assertNotNull(result.getTechnician());
        assertEquals(5L, result.getTechnician().getTechnicianId());
        assertEquals("Nguyễn Văn Kỹ Thuật", result.getTechnician().getFullName());

        verify(ticketRepository, times(1)).save(mockTicket);
        verify(statusLogRepository, times(1)).save(any(TicketStatusLog.class));
    }

    @Test
    @DisplayName("TC-02: Boundary Value - Cho phép đổi KTV khi lý do thay đổi đúng vừa đủ 10 ký tự (QT-07)")
    void TC02_assignTechnician_success_whenChangeReasonExactly10Chars() {
        // Given: Phiếu đã có KTV cũ và KTV mới hợp lệ
        Technician oldTechnician = Technician.builder()
                .technicianId(5L)
                .fullName("KTV Cũ")
                .centerId(1L)
                .build();
        mockTicket.setTechnician(oldTechnician);

        Technician newTechnician = Technician.builder()
                .technicianId(6L)
                .fullName("KTV Mới")
                .centerId(1L)
                .isActive(true)
                .build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        when(technicianRepository.findById(6L)).thenReturn(Optional.of(newTechnician));
        when(technicianRepository.findEligibleTechnicians(1L, 1)).thenReturn(List.of(newTechnician));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Giá trị biên: Đúng vừa đủ 10 ký tự
        String reason10Chars = "Doi KTV 10"; // Đúng vừa đủ 10 ký tự
        assertEquals(10, reason10Chars.length());

        // When & Then: Thực thi không ném ngoại lệ
        assertDoesNotThrow(() -> {
            Ticket updated = ticketService.assignTechnician(10L, 6L, 101L, reason10Chars);
            assertNotNull(updated);
            assertEquals(6L, updated.getTechnician().getTechnicianId());
            assertEquals("DA_PHAN_CONG", updated.getStatus());
        });

        verify(statusLogRepository, times(1)).save(any(TicketStatusLog.class));
    }

    @Test
    @DisplayName("TC-02: Boundary Value - Ném lỗi INVALID_CHANGE_REASON khi lý do đổi KTV chỉ có 9 ký tự (QT-07)")
    void TC02_assignTechnician_throwException_whenChangeReasonIs9Chars() {
        // Given: Phiếu đã có KTV cũ và KTV mới hợp lệ
        Technician oldTechnician = Technician.builder()
                .technicianId(5L)
                .fullName("KTV Cũ")
                .centerId(1L)
                .build();
        mockTicket.setTechnician(oldTechnician);

        Technician newTechnician = Technician.builder()
                .technicianId(6L)
                .fullName("KTV Mới")
                .centerId(1L)
                .isActive(true)
                .build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        when(technicianRepository.findById(6L)).thenReturn(Optional.of(newTechnician));
        when(technicianRepository.findEligibleTechnicians(1L, 1)).thenReturn(List.of(newTechnician));

        // Giá trị biên dưới: 9 ký tự (vi phạm quy tắc >= 10 ký tự)
        String reason9Chars = "Doi KTV m"; // Đúng 9 ký tự
        assertEquals(9, reason9Chars.length());

        // When & Then: Ném lỗi AppException với mã INVALID_CHANGE_REASON
        AppException exception = assertThrows(AppException.class, () ->
                ticketService.assignTechnician(10L, 6L, 101L, reason9Chars)
        );

        assertEquals("INVALID_CHANGE_REASON", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("từ 10 ký tự trở lên"));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }
}
