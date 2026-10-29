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

    @Mock
    private AppointmentService appointmentService;

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

        when(ticketRepository.findByTechnician_TechnicianIdAndDeletedAtIsNull(5L))
                .thenReturn(List.of(mockTicket));

        // When: KTV có ID = 5 tra cứu phiếu của mình
        List<com.mekongmobile.smartcrm.dto.TicketResponseDto> result = ticketService.getTicketsForTechnician(5L);

        // Then: Phiếu trả về được che 4 số giữa SĐT thành 090****567
        assertEquals(1, result.size());
        assertEquals("090****567", result.get(0).getCustomerPhone());
    }
    @Test
    @DisplayName("TC-07: getDeviceRepairHistory - Thiết bị có lịch sử, trả về DTO map đúng ngày (closed_at fallback received_at), KTV, nội dung xử lý")
    void TC07_getDeviceRepairHistory_withHistory() {
        LocalDateTime closedTime = LocalDateTime.of(2026, 6, 12, 16, 30);
        LocalDateTime receivedTime = LocalDateTime.of(2026, 6, 10, 9, 0);

        Ticket pastTicket1 = Ticket.builder()
                .ticketId(71L)
                .ticketCode("TK-202606-071")
                .device(mockDevice)
                .technician(mockTechnician)
                .resolution("Thay màn hình OLED chính hãng")
                .status("DONG")
                .receivedAt(receivedTime)
                .closedAt(closedTime)
                .build();

        // Phiếu 2 không có closed_at -> fallback sang received_at; resolution null -> fallback issueDesc
        Ticket pastTicket2 = Ticket.builder()
                .ticketId(98L)
                .ticketCode("TK-202609-098")
                .device(mockDevice)
                .technician(null)
                .issueDesc("Lỗi cáp kết nối")
                .resolution(null)
                .status("HOAN_TAT")
                .receivedAt(LocalDateTime.of(2026, 9, 1, 10, 0))
                .closedAt(null)
                .build();

        when(ticketRepository.findRepairHistory(eq(1L), eq(10L), any()))
                .thenReturn(List.of(pastTicket1, pastTicket2));

        List<com.mekongmobile.smartcrm.dto.DeviceRepairHistoryDto> history =
                ticketService.getDeviceRepairHistory(1L, 10L);

        assertEquals(2, history.size());
        assertEquals("TK-202606-071", history.get(0).getTicketCode());
        assertEquals("12/06/2026", history.get(0).getRepairedDate());
        assertEquals("Nguyễn Văn Kỹ Thuật", history.get(0).getTechnicianName());
        assertEquals("Thay màn hình OLED chính hãng", history.get(0).getSolutionDesc());

        assertEquals("TK-202609-098", history.get(1).getTicketCode());
        assertEquals("01/09/2026", history.get(1).getRepairedDate()); // Fallback receivedAt
        assertEquals("—", history.get(1).getTechnicianName()); // Fallback null tech
        assertEquals("Lỗi cáp kết nối", history.get(1).getSolutionDesc()); // Fallback issueDesc
    }

    @Test
    @DisplayName("TC-08: getDeviceRepairHistory - Thiết bị chưa từng sửa chữa trước đó, trả về danh sách rỗng")
    void TC08_getDeviceRepairHistory_emptyHistory() {
        when(ticketRepository.findRepairHistory(eq(2L), eq(10L), any()))
                .thenReturn(List.of());

        List<com.mekongmobile.smartcrm.dto.DeviceRepairHistoryDto> history =
                ticketService.getDeviceRepairHistory(2L, 10L);

        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    @DisplayName("TC-09: getDeviceRepairHistory - Thiết bị khác không lẫn dữ liệu và deviceId null an toàn")
    void TC09_getDeviceRepairHistory_isolationAndNullDevice() {
        when(ticketRepository.findRepairHistory(eq(1L), isNull(), any()))
                .thenReturn(List.of(Ticket.builder().ticketCode("TK-DEV-1").status("DONG").build()));

        List<com.mekongmobile.smartcrm.dto.DeviceRepairHistoryDto> historyDev1 =
                ticketService.getDeviceRepairHistory(1L, null);
        assertEquals(1, historyDev1.size());
        assertEquals("TK-DEV-1", historyDev1.get(0).getTicketCode());

        // deviceId = null -> trả về rỗng ngay mà không gọi repository
        List<com.mekongmobile.smartcrm.dto.DeviceRepairHistoryDto> nullResult =
                ticketService.getDeviceRepairHistory(null, 10L);
        assertTrue(nullResult.isEmpty());
        verify(ticketRepository, never()).findRepairHistory(isNull(), any(), any());
    }

    @Test
    @DisplayName("TC-10: getDeviceRepairHistory - Phiếu đang chọn được truyền vào excludeTicketId để bị loại trừ")
    void TC10_getDeviceRepairHistory_excludeCurrentTicket() {
        when(ticketRepository.findRepairHistory(eq(1L), eq(99L), any()))
                .thenReturn(List.of());

        ticketService.getDeviceRepairHistory(1L, 99L);

        verify(ticketRepository).findRepairHistory(eq(1L), eq(99L), any());
    }
    @Test
    @DisplayName("TC-11: countPreviousSameIssues - Chỉ đếm các phiếu đã đóng/hoàn tất (CLOSED_STATUSES), 2 phiếu đã đóng + 1 phiếu đang mở cùng thiết bị & nhóm thì repeatCount = 2")
    void TC11_calculateRepeatCount_countOnlyClosedTickets() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        when(ticketRepository.countPreviousSameIssues(eq(1L), eq(1), eq(10L), eq(TicketServiceImpl.CLOSED_STATUSES)))
                .thenReturn(2L);

        com.mekongmobile.smartcrm.dto.TicketResponseDto dto = ticketService.getTicketById(10L);

        assertNotNull(dto);
        assertEquals(2, dto.getRepeatCount());
        verify(ticketRepository).countPreviousSameIssues(eq(1L), eq(1), eq(10L), eq(TicketServiceImpl.CLOSED_STATUSES));
    }

    @Test
    @DisplayName("TC-12: updateTicket - Cập nhật thông tin phiếu và tính lại hạn SLA due_date khi thay đổi mức ưu tiên (QT-04)")
    void updateTicket_validPriorityChange_recalculatesDueDate() {
        LocalDateTime receivedAt = LocalDateTime.of(2026, 10, 20, 8, 0);
        mockTicket.setReceivedAt(receivedAt);
        mockTicket.setPriority("CAO");
        mockTicket.setDueDate(receivedAt.plusHours(24));

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        com.mekongmobile.smartcrm.dto.UpdateTicketRequestDto request = com.mekongmobile.smartcrm.dto.UpdateTicketRequestDto.builder()
                .issueDesc("Mô tả sự cố mới đã kiểm tra lại")
                .priority("THAP")
                .categoryId(1)
                .customerPhone("0908889999")
                .build();

        com.mekongmobile.smartcrm.dto.TicketResponseDto updated = ticketService.updateTicket(10L, request);

        assertNotNull(updated);
        assertEquals("THAP", updated.getPriority());
        assertEquals(receivedAt.plusHours(120), updated.getDueDate());
        assertEquals("Mô tả sự cố mới đã kiểm tra lại", updated.getIssueDesc());
        verify(ticketRepository).save(mockTicket);
    }

    @Test
    @DisplayName("TC-13: updateTicket - Ném lỗi TICKET_NOT_FOUND khi không tìm thấy phiếu hoặc phiếu đã bị xóa mềm")
    void updateTicket_ticketNotFound_throwsException() {
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        com.mekongmobile.smartcrm.dto.UpdateTicketRequestDto request = com.mekongmobile.smartcrm.dto.UpdateTicketRequestDto.builder()
                .issueDesc("Mô tả")
                .priority("CAO")
                .categoryId(1)
                .build();

        AppException ex = assertThrows(AppException.class, () -> ticketService.updateTicket(999L, request));
        assertEquals("TICKET_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-14: deleteTicket - Xóa mềm thành công khi phiếu ở trạng thái MOI và không có lịch hẹn hiệu lực (QT-13)")
    void deleteTicket_statusMoiAndNoActiveAppointment_success() {
        mockTicket.setStatus("MOI");
        mockTicket.setDeletedAt(null);

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        when(appointmentService.hasActiveAppointment(10L)).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> ticketService.deleteTicket(10L, 1L));

        assertNotNull(mockTicket.getDeletedAt());
        assertEquals("MOI", mockTicket.getStatus());
        verify(ticketRepository).save(mockTicket);
        verify(statusLogRepository).save(any(TicketStatusLog.class));
    }

    @Test
    @DisplayName("TC-15: deleteTicket - Ném lỗi TICKET_NOT_DELETABLE khi trạng thái phiếu khác MOI")
    void deleteTicket_statusNotMoi_throwsTicketNotDeletable() {
        mockTicket.setStatus("DANG_XU_LY");

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));

        AppException ex = assertThrows(AppException.class, () -> ticketService.deleteTicket(10L, 1L));
        assertEquals("TICKET_NOT_DELETABLE", ex.getErrorCode());
        assertNull(mockTicket.getDeletedAt());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("TC-16: deleteTicket - Ném lỗi TICKET_NOT_DELETABLE khi phiếu còn lịch hẹn có hiệu lực")
    void deleteTicket_hasActiveAppointment_throwsTicketNotDeletable() {
        mockTicket.setStatus("MOI");

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        when(appointmentService.hasActiveAppointment(10L)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> ticketService.deleteTicket(10L, 1L));
        assertEquals("TICKET_NOT_DELETABLE", ex.getErrorCode());
        assertNull(mockTicket.getDeletedAt());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("TC-17: getTicketById - Ném lỗi TICKET_NOT_FOUND khi phiếu đã bị xóa mềm (deleted_at != null)")
    void getTicketById_softDeletedTicket_throwsTicketNotFound() {
        mockTicket.setDeletedAt(LocalDateTime.now());
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));

        AppException ex = assertThrows(AppException.class, () -> ticketService.getTicketById(10L));
        assertEquals("TICKET_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-18: deleteTicket - Ném lỗi UNAUTHORIZED khi changedBy là null và verify không lưu hay ghi log")
    void deleteTicket_nullChangedBy_throwsUnauthorized() {
        AppException ex = assertThrows(AppException.class, () -> ticketService.deleteTicket(10L, null));
        assertEquals("UNAUTHORIZED", ex.getErrorCode());
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(statusLogRepository, never()).save(any(TicketStatusLog.class));
    }
}
