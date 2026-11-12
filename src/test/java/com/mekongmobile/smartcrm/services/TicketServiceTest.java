package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.entities.Device;
import com.mekongmobile.smartcrm.entities.IssueCategory;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.entities.Ticket;
import com.mekongmobile.smartcrm.entities.TicketStatusLog;
import com.mekongmobile.smartcrm.exceptions.AppException;
import com.mekongmobile.smartcrm.exceptions.BusinessRuleException;
import com.mekongmobile.smartcrm.dto.UpdateTicketStatusDto;
import com.mekongmobile.smartcrm.repositories.DeviceRepository;
import com.mekongmobile.smartcrm.repositories.IssueCategoryRepository;
import com.mekongmobile.smartcrm.repositories.TechnicianRepository;
import com.mekongmobile.smartcrm.repositories.TicketRepository;
import com.mekongmobile.smartcrm.repositories.TicketStatusLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    @DisplayName("TC-01 | QT-08 | Phân công KTV cùng trung tâm, đủ tay nghề -> DA_PHAN_CONG")
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
    @DisplayName("BUG-06: Quản lý trung tâm khác không sửa, gán hoặc xóa phiếu; không lưu hay ghi log")
    void managerFromOtherCenterCannotMutateTicket() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        var update = new com.mekongmobile.smartcrm.dto.UpdateTicketRequestDto();
        update.setIssueDesc("Nội dung bị sửa trái phép");
        var statusUpdate = UpdateTicketStatusDto.builder().newStatus("DA_PHAN_CONG").changedBy(99L).build();

        for (Runnable operation : List.<Runnable>of(
                () -> ticketService.updateTicket(10L, update, 2L),
                () -> ticketService.updateTicketStatus(10L, statusUpdate, 99L, null, 2L),
                () -> ticketService.assignTechnician(10L, 5L, 99L, "Điều phối", null, 2L),
                () -> ticketService.deleteTicket(10L, 99L, 2L))) {
            AppException ex = assertThrows(AppException.class, operation::run);
            assertEquals("FORBIDDEN_OTHER_CENTER", ex.getErrorCode());
        }

        assertEquals("MOI", mockTicket.getStatus());
        assertNull(mockTicket.getTechnician());
        assertEquals("Máy không sạc được pin", mockTicket.getIssueDesc());
        verify(ticketRepository, never()).save(any());
        verify(statusLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("BUG-06: Quản lý cùng trung tâm vẫn xem và phân công được phiếu")
    void managerFromOwnCenterCanReadAndAssignTicket() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(technicianRepository.findById(5L)).thenReturn(Optional.of(mockTechnician));
        when(technicianRepository.findEligibleTechnicians(1L, 1)).thenReturn(List.of(mockTechnician));
        when(statusLogRepository.save(any(TicketStatusLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(1L, ticketService.getTicketForManager(10L, 1L).getCenterId());
        Ticket assigned = ticketService.assignTechnician(10L, 5L, 7L, "Phân công KTV", null, 1L);

        assertEquals("DA_PHAN_CONG", assigned.getStatus());
        assertEquals(5L, assigned.getTechnician().getTechnicianId());
        verify(statusLogRepository).save(any(TicketStatusLog.class));
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
    @DisplayName("TC-10 | QT-08 | KTV không đủ điều kiện -> KTV_NOT_QUALIFIED, không lưu, không ghi log")
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
    @DisplayName("UT-04")
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
    @DisplayName("TC-07 | QT-15 | KTV xem phiếu cá nhân -> SĐT che 4 số giữa 090****567")
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
    @DisplayName("QT-15 | Không tạo SĐT giả khi ticket không có số điện thoại")
    void getTicketById_missingPhone_returnsNull() {
        mockTicket.setCustomerPhone(null);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));

        assertNull(ticketService.getTicketById(10L).getCustomerPhone());
    }
    @Test
    @DisplayName("UT-07: getDeviceRepairHistory - Thiết bị có lịch sử, trả về DTO map đúng ngày (closed_at fallback received_at), KTV, nội dung xử lý")
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
    @DisplayName("UT-08: getDeviceRepairHistory - Thiết bị chưa từng sửa chữa trước đó, trả về danh sách rỗng")
    void TC08_getDeviceRepairHistory_emptyHistory() {
        when(ticketRepository.findRepairHistory(eq(2L), eq(10L), any()))
                .thenReturn(List.of());

        List<com.mekongmobile.smartcrm.dto.DeviceRepairHistoryDto> history =
                ticketService.getDeviceRepairHistory(2L, 10L);

        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    @DisplayName("UT-09: getDeviceRepairHistory - Thiết bị khác không lẫn dữ liệu và deviceId null an toàn")
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
    @DisplayName("UT-10: getDeviceRepairHistory - Phiếu đang chọn được truyền vào excludeTicketId để bị loại trừ")
    void TC10_getDeviceRepairHistory_excludeCurrentTicket() {
        when(ticketRepository.findRepairHistory(eq(1L), eq(99L), any()))
                .thenReturn(List.of());

        ticketService.getDeviceRepairHistory(1L, 99L);

        verify(ticketRepository).findRepairHistory(eq(1L), eq(99L), any());
    }
    @Test
    @DisplayName("UT-11: countPreviousSameIssues - Chỉ đếm các phiếu đã đóng/hoàn tất (CLOSED_STATUSES), 2 phiếu đã đóng + 1 phiếu đang mở cùng thiết bị & nhóm thì repeatCount = 2")
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
    @DisplayName("TC-04 | QT-04 | Nhận thứ Bảy 15:00, mức CAO -> hạn thứ Hai 15:00 (bỏ Chủ nhật)")
    void TC04_dueDate_skipsSunday() {
    LocalDateTime received = LocalDateTime.of(2026, 11, 7, 15, 0); // thứ Bảy
    LocalDateTime due = ticketService.calculateDueDate(received, "CAO");
    assertEquals(LocalDateTime.of(2026, 11, 9, 15, 0), due);       // thứ Hai
}
    @Test
    @DisplayName("QT-04 | THAP tính năm ngày làm việc, bỏ qua Chủ nhật")
    void dueDate_lowPriority_skipsSunday() {
        LocalDateTime received = LocalDateTime.of(2026, 10, 20, 8, 0); // thứ Ba
        assertEquals(LocalDateTime.of(2026, 10, 26, 8, 0), ticketService.calculateDueDate(received, "THAP"));
    }

    @Test
    @DisplayName("QT-04 | TRUNG_BINH tính ba ngày làm việc")
    void dueDate_mediumPriority_threeWorkingDays() {
        LocalDateTime received = LocalDateTime.of(2026, 10, 19, 8, 0); // thứ Hai
        assertEquals(LocalDateTime.of(2026, 10, 22, 8, 0), ticketService.calculateDueDate(received, "TRUNG_BINH"));
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
        assertEquals(LocalDateTime.of(2026, 10, 26, 8, 0), updated.getDueDate());
        assertEquals("Mô tả sự cố mới đã kiểm tra lại", updated.getIssueDesc());
        verify(ticketRepository).save(mockTicket);
    }

    @Test
    @DisplayName("UT-13: updateTicket - Ném lỗi TICKET_NOT_FOUND khi không tìm thấy phiếu hoặc phiếu đã bị xóa mềm")
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
    @DisplayName("TC-06 | QT-13 | Xóa mềm phiếu MOI -> deleted_at có giá trị, log DA_XOA")
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
        ArgumentCaptor<TicketStatusLog> captor = ArgumentCaptor.forClass(TicketStatusLog.class);
        verify(statusLogRepository).save(captor.capture());
        assertEquals("MOI", captor.getValue().getFromStatus());
        assertEquals("DA_XOA", captor.getValue().getToStatus());
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
    @DisplayName("UT-16: deleteTicket - Ném lỗi TICKET_NOT_DELETABLE khi phiếu còn lịch hẹn có hiệu lực")
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
    @DisplayName("UT-17: getTicketById - Ném lỗi TICKET_NOT_FOUND khi phiếu đã bị xóa mềm (deleted_at != null)")
    void getTicketById_softDeletedTicket_throwsTicketNotFound() {
        mockTicket.setDeletedAt(LocalDateTime.now());
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));

        AppException ex = assertThrows(AppException.class, () -> ticketService.getTicketById(10L));
        assertEquals("TICKET_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("UT-18: deleteTicket - Ném lỗi UNAUTHORIZED khi changedBy là null và verify không lưu hay ghi log")
    void deleteTicket_nullChangedBy_throwsUnauthorized() {
        AppException ex = assertThrows(AppException.class, () -> ticketService.deleteTicket(10L, null));
        assertEquals("UNAUTHORIZED", ex.getErrorCode());
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(statusLogRepository, never()).save(any(TicketStatusLog.class));
    }

    @Nested
    @DisplayName("TC-08: Kiểm thử phân loại trạng thái SLA (getSlaStatus)")
    class TC08_SlaStatusClassificationTests {

        @Test
        @DisplayName("TC-08: Ticket with due_date < NOW() returns status 'QUA_HAN'")
        void TC08_getSlaStatus_dueDatePast_returnsQuaHan() {
            LocalDateTime now = LocalDateTime.of(2026, 11, 8, 12, 0, 0);
            LocalDateTime pastDueDate = LocalDateTime.of(2026, 10, 22, 17, 0, 0);

            String status = ticketService.getSlaStatus(pastDueDate, now);

            assertEquals("QUA_HAN", status);
        }

        @Test
        @DisplayName("UT-08: Ticket within 24h of due_date returns status 'SAP_QUA_HAN'")
        void TC08_getSlaStatus_dueDateWithin24h_returnsSapQuaHan() {
            LocalDateTime now = LocalDateTime.of(2026, 11, 8, 12, 0, 0);
            LocalDateTime nearDueDate = now.plusHours(12);

            String status = ticketService.getSlaStatus(nearDueDate, now);

            assertEquals("SAP_QUA_HAN", status);

            // Biên chính xác 24 giờ
            assertEquals("SAP_QUA_HAN", ticketService.getSlaStatus(now.plusHours(24), now));
        }

        @Test
        @DisplayName("UT-08: Ticket far from due_date returns status 'CON_HAN'")
        void TC08_getSlaStatus_dueDateFar_returnsConHan() {
            LocalDateTime now = LocalDateTime.of(2026, 11, 8, 12, 0, 0);
            LocalDateTime farDueDate = now.plusDays(5);

            String status = ticketService.getSlaStatus(farDueDate, now);

            assertEquals("CON_HAN", status);

            // Biên vượt quá 24 giờ 1 phút
            assertEquals("CON_HAN", ticketService.getSlaStatus(now.plusHours(24).plusMinutes(1), now));
        }

        @Test
        @DisplayName("TC-08: Ticket with null due_date returns status 'CON_HAN'")
        void TC08_getSlaStatus_nullDueDate_returnsConHan() {
            assertEquals("CON_HAN", ticketService.getSlaStatus(null, LocalDateTime.now()));
        }
    }

    @Nested
    @DisplayName("QT-L4-01: Kiểm thử ràng buộc thời gian hẹn không được ở trong quá khứ")
    class AppointmentTimeValidationTests {

        @Test
        @DisplayName("QT-L4-01: Từ chối phân công khi thời gian hẹn ở trong quá khứ")
        void assignTechnician_pastAppointmentTime_throwsInvalidAppointmentTime() {
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
            when(technicianRepository.findById(5L)).thenReturn(Optional.of(mockTechnician));
            when(technicianRepository.findEligibleTechnicians(1L, 1)).thenReturn(List.of(mockTechnician));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
            LocalDateTime pastTime = LocalDateTime.now().minusDays(1);

            AppException ex = assertThrows(AppException.class, () ->
                    ticketService.assignTechnician(10L, 5L, 101L, "Phân công kiểm tra", pastTime)
            );

            assertEquals("INVALID_APPOINTMENT_TIME", ex.getErrorCode());
            assertTrue(ex.getMessage().contains("phải ở trong tương lai"));
            verify(appointmentService, never()).scheduleAppointment(any(Ticket.class), any(Technician.class), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("QT-L4-01: Chấp nhận phân công khi thời gian hẹn ở trong tương lai")
        void assignTechnician_futureAppointmentTime_succeeds() {
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
            when(technicianRepository.findById(5L)).thenReturn(Optional.of(mockTechnician));
            when(technicianRepository.findEligibleTechnicians(1L, 1)).thenReturn(List.of(mockTechnician));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

            LocalDateTime futureTime = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0);

            Ticket result = ticketService.assignTechnician(10L, 5L, 101L, "Phân công kiểm tra", futureTime);

            assertNotNull(result);
            assertEquals("DA_PHAN_CONG", result.getStatus());
            verify(appointmentService, times(1)).scheduleAppointment(any(Ticket.class), eq(mockTechnician), eq(futureTime));
        }
    }
    private void givenReassignScenario() {
    Technician oldTech = Technician.builder()
            .technicianId(5L).fullName("KTV Cũ").centerId(1L).build();
    mockTicket.setTechnician(oldTech);
    Technician newTech = Technician.builder()
            .technicianId(6L).fullName("KTV Mới").centerId(1L).isActive(true).build();

    lenient().when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
    lenient().when(technicianRepository.findById(6L)).thenReturn(Optional.of(newTech));
    lenient().when(technicianRepository.findEligibleTechnicians(1L, 1)).thenReturn(List.of(newTech));
    lenient().when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));
}

@Test
@DisplayName("TC-02 | QT-07 | Biên: lý do đổi KTV đúng 10 ký tự -> thành công")
void TC02_changeReason_exactly10_ok() {
    givenReassignScenario();
    String reason = "Doi KTV 10";
    assertEquals(10, reason.length());

    Ticket result = ticketService.assignTechnician(10L, 6L, 101L, reason);

    assertEquals(6L, result.getTechnician().getTechnicianId());
    assertEquals("DA_PHAN_CONG", result.getStatus());
    verify(statusLogRepository, times(1)).save(any(TicketStatusLog.class));
}

@Test
@DisplayName("TC-03 | QT-07 | Biên: lý do đổi KTV 9 ký tự -> INVALID_CHANGE_REASON, không lưu")
void TC03_changeReason_9chars_rejected() {
    givenReassignScenario();
    String reason = "Doi KTV m";
    assertEquals(9, reason.length());

    AppException ex = assertThrows(AppException.class,
            () -> ticketService.assignTechnician(10L, 6L, 101L, reason));

    assertEquals("INVALID_CHANGE_REASON", ex.getErrorCode());
    verify(ticketRepository, never()).save(any(Ticket.class));
    verify(statusLogRepository, never()).save(any(TicketStatusLog.class));
}
@Test
@DisplayName("TC-08 | QT-06 | Nhảy cóc DA_PHAN_CONG -> HOAN_TAT phải bị từ chối, không lưu, không ghi log")
void TC08_updateStatus_skipStep_rejected() {
    mockTicket.setStatus("DA_PHAN_CONG");
    lenient().when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
    lenient().when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));

    com.mekongmobile.smartcrm.dto.UpdateTicketStatusDto dto =
            com.mekongmobile.smartcrm.dto.UpdateTicketStatusDto.builder()
                    .newStatus("HOAN_TAT").changedBy(2L).build();

    assertThrows(RuntimeException.class, () -> ticketService.updateTicketStatus(10L, dto));
    assertEquals("DA_PHAN_CONG", mockTicket.getStatus());
    verify(ticketRepository, never()).save(any(Ticket.class));
    verify(statusLogRepository, never()).save(any(TicketStatusLog.class));
}

@Test
@DisplayName("QT-06 | Từ chối trạng thái đích không nằm trong quy trình")
void updateStatus_unknownTarget_rejected() {
    mockTicket.setStatus("DA_PHAN_CONG");
    when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
    UpdateTicketStatusDto dto = UpdateTicketStatusDto.builder().newStatus("ABC").changedBy(2L).build();

    assertThrows(BusinessRuleException.class, () -> ticketService.updateTicketStatus(10L, dto));

    assertEquals("DA_PHAN_CONG", mockTicket.getStatus());
    verify(ticketRepository, never()).save(any(Ticket.class));
    verify(statusLogRepository, never()).save(any(TicketStatusLog.class));
}

@Test
@DisplayName("QT-06 | DA_PHAN_CONG -> DANG_XU_LY hợp lệ và ghi đúng một log")
void updateStatus_nextAllowedStep_succeedsAndLogsOnce() {
    mockTicket.setStatus("DA_PHAN_CONG");
    when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
    when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));
    UpdateTicketStatusDto dto = UpdateTicketStatusDto.builder().newStatus("DANG_XU_LY").changedBy(2L).build();

    ticketService.updateTicketStatus(10L, dto);

    assertEquals("DANG_XU_LY", mockTicket.getStatus());
    verify(ticketRepository, times(1)).save(mockTicket);
    verify(statusLogRepository, times(1)).save(any(TicketStatusLog.class));
}
}
