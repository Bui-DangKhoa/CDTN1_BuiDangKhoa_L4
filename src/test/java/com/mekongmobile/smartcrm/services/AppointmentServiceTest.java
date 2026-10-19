package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.entities.Appointment;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.entities.Ticket;
import com.mekongmobile.smartcrm.exceptions.AppException;
import com.mekongmobile.smartcrm.repositories.AppointmentRepository;
import com.mekongmobile.smartcrm.repositories.TechnicianRepository;
import com.mekongmobile.smartcrm.repositories.TicketRepository;
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
 * Unit Test cho AppointmentService (QT-L4-01 / NFR2: Chống trùng lịch hẹn KTV).
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TechnicianRepository technicianRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private Ticket mockTicket;
    private Technician mockTechnician;
    private LocalDateTime appointmentTime;

    @BeforeEach
    void setUp() {
        mockTicket = Ticket.builder()
                .ticketId(10L)
                .ticketCode("TCK-2026-001")
                .centerId(1L)
                .build();

        mockTechnician = Technician.builder()
                .technicianId(5L)
                .fullName("Trần Kỹ Thuật")
                .centerId(1L)
                .build();

        appointmentTime = LocalDateTime.of(2026, 10, 20, 9, 0);
    }

    @Test
    @DisplayName("TC-03: Invalid Data & Exception - Từ chối đặt lịch khi KTV bị trùng lịch hẹn (QT-L4-01)")
    void TC03_scheduleAppointment_throwException_whenScheduleConflict() {
        // Given: KTV đã có lịch hẹn khác trong cùng khung giờ (chưa bị hủy)
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        when(technicianRepository.findById(5L)).thenReturn(Optional.of(mockTechnician));
        when(appointmentRepository.existsByTechnician_TechnicianIdAndAppointmentTimeAndStatusNot(5L, appointmentTime, "DA_HUY"))
                .thenReturn(true);

        // When & Then: Ném lỗi AppException với mã SCHEDULE_CONFLICT
        AppException exception = assertThrows(AppException.class, () ->
                appointmentService.scheduleAppointment(10L, 5L, appointmentTime)
        );

        assertEquals("SCHEDULE_CONFLICT", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("đã có lịch hẹn khác"));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("QT-L4-01: Đặt lịch hẹn thành công khi KTV không có lịch trùng")
    void testScheduleAppointment_Success() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        when(technicianRepository.findById(5L)).thenReturn(Optional.of(mockTechnician));
        when(appointmentRepository.existsByTechnician_TechnicianIdAndAppointmentTimeAndStatusNot(5L, appointmentTime, "DA_HUY"))
                .thenReturn(false);

        Appointment savedAppointment = Appointment.builder()
                .appointmentId(100L)
                .ticket(mockTicket)
                .technician(mockTechnician)
                .appointmentTime(appointmentTime)
                .status("CHO_XAC_NHAN")
                .build();
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppointment);

        Appointment result = appointmentService.scheduleAppointment(10L, 5L, appointmentTime);

        assertNotNull(result);
        assertEquals("CHO_XAC_NHAN", result.getStatus());
        assertEquals(mockTechnician, result.getTechnician());
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Hủy lịch hẹn thành công và đổi trạng thái sang DA_HUY")
    void testCancelAppointment_Success() {
        Appointment appointment = Appointment.builder()
                .appointmentId(100L)
                .ticket(mockTicket)
                .technician(mockTechnician)
                .appointmentTime(appointmentTime)
                .status("CHO_XAC_NHAN")
                .build();

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment cancelled = appointmentService.cancelAppointment(100L);

        assertNotNull(cancelled);
        assertEquals("DA_HUY", cancelled.getStatus());
        verify(appointmentRepository, times(1)).save(appointment);
    }
}
