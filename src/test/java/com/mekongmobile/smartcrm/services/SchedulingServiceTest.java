package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.dto.ScheduleAppointmentDto;
import com.mekongmobile.smartcrm.dto.TechnicianDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.entities.*;
import com.mekongmobile.smartcrm.exceptions.BusinessRuleException;
import com.mekongmobile.smartcrm.repositories.AppointmentRepository;
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
 * Unit Test cho SchedulingService (QT-07: Điều phối lịch hẹn kỹ thuật viên).
 */
@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TechnicianRepository technicianRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private TicketStatusLogRepository statusLogRepository;

    @InjectMocks
    private SchedulingServiceImpl schedulingService;

    private Ticket mockTicket;
    private Technician mockTechnician;
    private LocalDateTime appointmentTime;

    @BeforeEach
    void setUp() {
        IssueCategory mockCategory = IssueCategory.builder()
                .categoryId(1)
                .categoryName("Lỗi Mainboard")
                .defaultPriority("CAO")
                .build();

        Device mockDevice = Device.builder()
                .deviceId(1L)
                .customerId(101L)
                .deviceName("Samsung Galaxy S24")
                .serialNo("SS-8899")
                .build();

        mockTicket = Ticket.builder()
                .ticketId(10L)
                .ticketCode("TCK-2026-002")
                .customerId(101L)
                .centerId(1L)
                .category(mockCategory)
                .device(mockDevice)
                .priority("CAO")
                .status("MOI")
                .dueDate(LocalDateTime.now().plusDays(2))
                .build();

        mockTechnician = Technician.builder()
                .technicianId(5L)
                .employeeId(1005L)
                .fullName("Trần Kỹ Thuật")
                .centerId(1L)
                .level("LEVEL_3")
                .isActive(true)
                .build();

        appointmentTime = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
    }

    @Test
    @DisplayName("QT-07: Tìm kiếm kỹ thuật viên đủ điều kiện thành công (lọc người trùng lịch)")
    void testFindEligibleTechnicians_Success() {
        Technician busyTechnician = Technician.builder()
                .technicianId(6L)
                .employeeId(1006L)
                .fullName("Lê Bận Rộn")
                .centerId(1L)
                .level("LEVEL_2")
                .isActive(true)
                .build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        when(technicianRepository.findEligibleTechnicians(1L, 1)).thenReturn(List.of(mockTechnician, busyTechnician));
        
        // mockTechnician rảnh (false), busyTechnician bị trùng lịch (true)
        when(appointmentRepository.existsActiveAppointmentAtTime(5L, appointmentTime)).thenReturn(false);
        when(appointmentRepository.existsActiveAppointmentAtTime(6L, appointmentTime)).thenReturn(true);

        List<TechnicianDto> result = schedulingService.findEligibleTechnicians(10L, appointmentTime);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Trần Kỹ Thuật", result.get(0).getFullName());
    }

    @Test
    @DisplayName("QT-07: Điều phối lịch hẹn kỹ thuật viên thành công, đổi trạng thái sang DA_PHAN_CONG")
    void testScheduleAppointment_Success() {
        ScheduleAppointmentDto scheduleDto = ScheduleAppointmentDto.builder()
                .ticketId(10L)
                .technicianId(5L)
                .appointmentTime(appointmentTime)
                .scheduledBy(99L)
                .build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        when(technicianRepository.findById(5L)).thenReturn(Optional.of(mockTechnician));
        when(appointmentRepository.existsActiveAppointmentAtTime(5L, appointmentTime)).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);

        TicketResponseDto response = schedulingService.scheduleAppointment(scheduleDto);

        assertNotNull(response);
        assertEquals("DA_PHAN_CONG", mockTicket.getStatus());
        assertEquals(mockTechnician, mockTicket.getTechnician());
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
        verify(statusLogRepository, times(1)).save(any(TicketStatusLog.class));
    }

    @Test
    @DisplayName("QT-07: Phát hiện xung đột trùng lịch hẹn và ném ngoại lệ BusinessRuleException")
    void testScheduleAppointment_ConflictDetected_ThrowsException() {
        ScheduleAppointmentDto scheduleDto = ScheduleAppointmentDto.builder()
                .ticketId(10L)
                .technicianId(5L)
                .appointmentTime(appointmentTime)
                .scheduledBy(99L)
                .build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        when(technicianRepository.findById(5L)).thenReturn(Optional.of(mockTechnician));
        when(appointmentRepository.existsActiveAppointmentAtTime(5L, appointmentTime)).thenReturn(true);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, 
                () -> schedulingService.scheduleAppointment(scheduleDto));
        
        assertTrue(exception.getMessage().contains("đã có lịch hẹn khác"));
        verify(appointmentRepository, never()).save(any());
    }
}
