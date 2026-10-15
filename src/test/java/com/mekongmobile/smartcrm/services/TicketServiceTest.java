package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.dto.TicketRequestDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.dto.UpdateTicketStatusDto;
import com.mekongmobile.smartcrm.entities.Device;
import com.mekongmobile.smartcrm.entities.IssueCategory;
import com.mekongmobile.smartcrm.entities.Ticket;
import com.mekongmobile.smartcrm.exceptions.BusinessRuleException;
import com.mekongmobile.smartcrm.repositories.DeviceRepository;
import com.mekongmobile.smartcrm.repositories.IssueCategoryRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho TicketService (QT-04: Tiếp nhận sự cố & QT-08: Cập nhật tiến độ).
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private IssueCategoryRepository categoryRepository;

    @Mock
    private TicketStatusLogRepository statusLogRepository;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private Device mockDevice;
    private IssueCategory mockCategory;
    private Ticket mockTicket;

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
                .receivedAt(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusHours(24))
                .build();
    }

    @Test
    @DisplayName("QT-04: Tạo ticket thành công và tự gán độ ưu tiên mặc định theo danh mục sự cố")
    void testCreateTicket_Success() {
        TicketRequestDto request = TicketRequestDto.builder()
                .ticketCode("TCK-2026-001")
                .customerId(101L)
                .deviceId(1L)
                .centerId(1L)
                .categoryId(1)
                .issueDesc("Máy không nhận sạc")
                .priority(null) // Để trống để kiểm tra fallback vào defaultPriority của category
                .build();

        when(ticketRepository.findByTicketCode("TCK-2026-001")).thenReturn(Optional.empty());
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(mockDevice));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(mockCategory));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);

        TicketResponseDto response = ticketService.createTicket(request);

        assertNotNull(response);
        assertEquals("TCK-2026-001", response.getTicketCode());
        assertEquals("CAO", response.getPriority());
        assertEquals("MOI", response.getStatus());
        verify(statusLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("QT-08: Cập nhật trạng thái ticket sang DANG_XU_LY thành công")
    void testUpdateTicketStatus_Success() {
        UpdateTicketStatusDto updateDto = UpdateTicketStatusDto.builder()
                .newStatus("DANG_XU_LY")
                .changedBy(201L)
                .changeReason("Kỹ thuật viên đã nhận máy và bắt đầu kiểm tra linh kiện")
                .build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);

        TicketResponseDto result = ticketService.updateTicketStatus(10L, updateDto);

        assertNotNull(result);
        assertEquals("DANG_XU_LY", mockTicket.getStatus());
        verify(statusLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("QT-08: Ném ngoại lệ khi cập nhật trạng thái mới trùng với trạng thái cũ")
    void testUpdateTicketStatus_SameStatus_ThrowsException() {
        UpdateTicketStatusDto updateDto = UpdateTicketStatusDto.builder()
                .newStatus("MOI") // Trùng với mockTicket.status ("MOI")
                .changedBy(201L)
                .build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));

        assertThrows(BusinessRuleException.class, () -> ticketService.updateTicketStatus(10L, updateDto));
    }
}
