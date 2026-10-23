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
    @DisplayName("Happy Path: testAssignTechnician_Success - Phân công KTV cùng center, proficiency >= 3 thành công, status chuyển sang DA_PHAN_CONG")
    void testAssignTechnician_Success() {
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
    @DisplayName("TC-01: Happy Path - Phân công KTV đủ tay nghề thành công lần đầu cho phiếu ở trạng thái MOI")
    void TC01_assignTechnician_success_whenValidData() {
        testAssignTechnician_Success();
    }

    @Test
    @DisplayName("Boundary Value: testAssignTechnician_ChangeReasonBoundary - Đổi KTV cũ với changeReason đúng 10 ký tự -> PASS; dưới 10 ký tự -> ném AppException (QT-07)")
    void testAssignTechnician_ChangeReasonBoundary() {
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

        // 1. Biên đúng vừa đủ 10 ký tự -> PASS
        String reason10Chars = "Doi KTV 10"; // Đúng vừa đủ 10 ký tự
        assertEquals(10, reason10Chars.length());
        assertDoesNotThrow(() -> {
            Ticket updated = ticketService.assignTechnician(10L, 6L, 101L, reason10Chars);
            assertNotNull(updated);
            assertEquals(6L, updated.getTechnician().getTechnicianId());
            assertEquals("DA_PHAN_CONG", updated.getStatus());
        });

        // 2. Reset KTV cũ về oldTechnician (ID 5), rồi đổi sang KTV mới (ID 6) với lý do 9 ký tự (< 10) -> ném AppException
        mockTicket.setTechnician(oldTechnician);
        String reason9Chars = "Doi KTV m"; // 9 ký tự (< 10)
        assertEquals(9, reason9Chars.length());
        AppException exception = assertThrows(AppException.class, () ->
                ticketService.assignTechnician(10L, 6L, 101L, reason9Chars)
        );
        assertEquals("INVALID_CHANGE_REASON", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("từ 10 ký tự trở lên"));
    }

    @Test
    @DisplayName("TC-02: Boundary Value - Cho phép đổi KTV khi lý do thay đổi đúng vừa đủ 10 ký tự (QT-07)")
    void TC02_assignTechnician_success_whenChangeReasonExactly10Chars() {
        testAssignTechnician_ChangeReasonBoundary();
    }

    @Test
    @DisplayName("TC-02: Boundary Value - Ném lỗi INVALID_CHANGE_REASON khi lý do đổi KTV chỉ có 9 ký tự (QT-07)")
    void TC02_assignTechnician_throwException_whenChangeReasonIs9Chars() {
        testAssignTechnician_ChangeReasonBoundary();
    }

    @Test
    @DisplayName("Invalid Data / Exception: testAssignTechnician_InvalidProficiency_QT08 - Gán KTV có proficiency < 3 -> ném AppException(KTV_NOT_QUALIFIED) (QT-08)")
    void testAssignTechnician_InvalidProficiency_QT08() {
        // Given: KTV không nằm trong danh sách đủ điều kiện của trung tâm và danh mục sự cố
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        when(technicianRepository.findById(5L)).thenReturn(Optional.of(mockTechnician));
        when(technicianRepository.findEligibleTechnicians(1L, 1)).thenReturn(List.of()); // Không đủ điều kiện

        // When & Then: Ném lỗi AppException với mã KTV_NOT_QUALIFIED
        AppException exception = assertThrows(AppException.class, () ->
                ticketService.assignTechnician(10L, 5L, 101L, "Phân công kiểm tra")
        );

        assertEquals("KTV_NOT_QUALIFIED", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("chưa đạt mức 3") || exception.getMessage().contains("KTV không cùng trung tâm"));
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(statusLogRepository, never()).save(any(TicketStatusLog.class));
    }

    @Test
    @DisplayName("TC-04: Invalid Data - Ném lỗi KTV_NOT_QUALIFIED khi KTV khác trung tâm hoặc tay nghề < 3 (QT-08)")
    void TC04_assignTechnician_throwException_whenKtvNotQualified() {
        testAssignTechnician_InvalidProficiency_QT08();
    }

    @Test
    @DisplayName("TC-05: QT-14 - Quản lý tra cứu phiếu theo phân vùng trung tâm center_id (giữ nguyên SĐT)")
    void TC05_getUnassignedTicketsSortedByDueDate_managerScoping() {
        mockTicket.setCustomerPhone("0901234567");

        Ticket otherCenterTicket = Ticket.builder()
                .ticketId(11L)
                .ticketCode("TCK-2026-002")
                .customerId(102L)
                .centerId(2L) // Khác centerId
                .customerPhone("0912345678")
                .status("MOI")
                .receivedAt(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusHours(48))
                .build();

        when(ticketRepository.findUnassignedTicketsOrderByDueDateAsc())
                .thenReturn(List.of(mockTicket, otherCenterTicket));

        // When: Quản lý của centerId = 1 tra cứu
        List<com.mekongmobile.smartcrm.dto.TicketResponseDto> result = ticketService.getUnassignedTicketsSortedByDueDate(1L);

        // Then: Chỉ lấy phiếu thuộc centerId = 1, SĐT giữ nguyên
        assertEquals(1, result.size());
        assertEquals("TCK-2026-001", result.get(0).getTicketCode());
        assertEquals("0901234567", result.get(0).getCustomerPhone());
    }

    @Test
    @DisplayName("TC-06: QT-15 - Kỹ thuật viên tra cứu phiếu cá nhân, tự động che 4 số giữa SĐT dạng 090****567")
    void TC06_getTicketsForTechnician_maskPhone() {
        mockTicket.setCustomerPhone("0901234567");
        mockTicket.setTechnician(mockTechnician);

        when(ticketRepository.findByTechnician_TechnicianId(5L))
                .thenReturn(List.of(mockTicket));

        // When: KTV có ID = 5 tra cứu phiếu của mình
        List<com.mekongmobile.smartcrm.dto.TicketResponseDto> result = ticketService.getTicketsForTechnician(5L);

        // Then: Phiếu trả về được che 4 số giữa SĐT thành 090****567
        assertEquals(1, result.size());
        assertEquals("090****567", result.get(0).getCustomerPhone());
    }
}

