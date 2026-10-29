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
 *
 * @author MekongMobile CRM Team
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
    @DisplayName("Luồng ngoại lệ - QT-L4-01: Đặt lịch trùng khung giờ KTV đã có hẹn -> ném AppException(SCHEDULE_CONFLICT)")
    void testCreateAppointment_ScheduleConflict_QTL401() {
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
    @DisplayName("Happy Path - QT-L4-01: Đặt lịch hẹn không trùng khung giờ thành công")
    void testCreateAppointment_Success() {
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
    @DisplayName("QT-L4-01: Đặt lịch hẹn thành công khi KTV không có lịch trùng")
    void testScheduleAppointment_Success() {
        testCreateAppointment_Success();
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
    @Test
    @DisplayName("TC-APPT-06: findAppointmentsByKeyword - Che 4 số giữa SĐT khách hàng theo QT-15 qua MaskingUtil")
    void testFindAppointmentsByKeyword_MasksPhoneNumber() {
        mockTicket.setCustomerPhone("0901234567");
        Appointment app = Appointment.builder()
                .appointmentId(100L)
                .ticket(mockTicket)
                .technician(mockTechnician)
                .appointmentTime(appointmentTime)
                .status("CHO_XAC_NHAN")
                .build();

        when(appointmentRepository.findByTicketCodeOrPhone("TCK-2026-001")).thenReturn(java.util.List.of(app));

        java.util.List<com.mekongmobile.smartcrm.dto.AppointmentResponseDto> dtos =
                appointmentService.findAppointmentsByKeyword("TCK-2026-001");

        assertNotNull(dtos);
        assertEquals(1, dtos.size());
        assertEquals("090****567", dtos.get(0).getCustomerPhone());
        assertFalse(dtos.get(0).getCustomerPhone().contains("1234"));
    }

    @Test
    @DisplayName("TC-APPT-07: getAppointmentById - Trả về DTO thông tin lịch hẹn khi tồn tại")
    void getAppointmentById_valid_returnsDto() {
        Appointment app = Appointment.builder()
                .appointmentId(100L)
                .ticket(mockTicket)
                .technician(mockTechnician)
                .appointmentTime(appointmentTime)
                .status("CHO_XAC_NHAN")
                .build();

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(app));

        com.mekongmobile.smartcrm.dto.AppointmentResponseDto dto = appointmentService.getAppointmentById(100L);

        assertNotNull(dto);
        assertEquals(100L, dto.getAppointmentId());
        assertEquals("CHO_XAC_NHAN", dto.getStatus());
    }

    @Test
    @DisplayName("TC-APPT-08: getAppointmentById - Ném lỗi APPOINTMENT_NOT_FOUND khi không tìm thấy lịch hẹn")
    void getAppointmentById_notFound_throwsException() {
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> appointmentService.getAppointmentById(999L));
        assertEquals("APPOINTMENT_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-APPT-09: updateAppointment - Đổi giờ nhẹ không tự trùng chính mình (QT-L4-01 loại trừ self ID)")
    void updateAppointment_slightTimeAdjustment_noSelfConflict_success() {
        Appointment app = Appointment.builder()
                .appointmentId(100L)
                .ticket(mockTicket)
                .technician(mockTechnician)
                .appointmentTime(appointmentTime) // 09:00
                .status("CHO_XAC_NHAN")
                .build();

        LocalDateTime newTime = LocalDateTime.of(2026, 10, 20, 9, 5); // Đổi nhẹ sang 09:05
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(app));
        when(appointmentRepository.existsActiveAppointmentInWindowExcludingSelf(
                eq(5L), eq(newTime.minusMinutes(30)), eq(newTime.plusMinutes(30)), eq(100L)))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        com.mekongmobile.smartcrm.dto.UpdateAppointmentRequest request = com.mekongmobile.smartcrm.dto.UpdateAppointmentRequest.builder()
                .appointmentTime(newTime)
                .build();

        com.mekongmobile.smartcrm.dto.AppointmentResponseDto updated = appointmentService.updateAppointment(100L, request);

        assertNotNull(updated);
        assertEquals(newTime, updated.getAppointmentTime());
        verify(appointmentRepository).save(app);
    }

    @Test
    @DisplayName("TC-APPT-10: updateAppointment - Xung đột trong khung giờ < 30 phút với lịch hẹn khác -> ném SCHEDULE_CONFLICT")
    void updateAppointment_inside30MinWindow_throwsScheduleConflict() {
        Appointment app = Appointment.builder()
                .appointmentId(100L)
                .ticket(mockTicket)
                .technician(mockTechnician)
                .appointmentTime(appointmentTime) // 09:00
                .status("CHO_XAC_NHAN")
                .build();

        LocalDateTime newTime = LocalDateTime.of(2026, 10, 20, 9, 45); // KTV có hẹn khác lúc 10:00 (cách 15 phút < 30)
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(app));
        when(appointmentRepository.existsActiveAppointmentInWindowExcludingSelf(
                eq(5L), eq(newTime.minusMinutes(30)), eq(newTime.plusMinutes(30)), eq(100L)))
                .thenReturn(true);

        com.mekongmobile.smartcrm.dto.UpdateAppointmentRequest request = com.mekongmobile.smartcrm.dto.UpdateAppointmentRequest.builder()
                .appointmentTime(newTime)
                .build();

        AppException ex = assertThrows(AppException.class, () -> appointmentService.updateAppointment(100L, request));
        assertEquals("SCHEDULE_CONFLICT", ex.getErrorCode());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("TC-APPT-11: updateAppointment - Đổi giờ tại đúng biên 30 phút so với lịch hẹn khác -> hợp lệ thành công")
    void updateAppointment_boundaryExact30Min_success() {
        Appointment app = Appointment.builder()
                .appointmentId(100L)
                .ticket(mockTicket)
                .technician(mockTechnician)
                .appointmentTime(appointmentTime) // 09:00
                .status("CHO_XAC_NHAN")
                .build();

        LocalDateTime boundaryTime = LocalDateTime.of(2026, 10, 20, 9, 30); // Đúng 30 phút trước 10:00
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(app));
        when(appointmentRepository.existsActiveAppointmentInWindowExcludingSelf(
                eq(5L), eq(boundaryTime.minusMinutes(30)), eq(boundaryTime.plusMinutes(30)), eq(100L)))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        com.mekongmobile.smartcrm.dto.UpdateAppointmentRequest request = com.mekongmobile.smartcrm.dto.UpdateAppointmentRequest.builder()
                .appointmentTime(boundaryTime)
                .build();

        com.mekongmobile.smartcrm.dto.AppointmentResponseDto updated = appointmentService.updateAppointment(100L, request);

        assertNotNull(updated);
        assertEquals(boundaryTime, updated.getAppointmentTime());
        verify(appointmentRepository).save(app);
    }

    @Test
    @DisplayName("TC-APPT-12: cancelAppointment - Ném lỗi APPOINTMENT_NOT_DELETABLE khi lịch hẹn đã được khách hàng xác nhận (DA_XAC_NHAN)")
    void cancelAppointment_statusDaXacNhan_throwsAppointmentNotDeletable() {
        Appointment app = Appointment.builder()
                .appointmentId(100L)
                .ticket(mockTicket)
                .technician(mockTechnician)
                .appointmentTime(appointmentTime)
                .status("DA_XAC_NHAN")
                .build();

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(app));

        AppException ex = assertThrows(AppException.class, () -> appointmentService.cancelAppointment(100L));
        assertEquals("APPOINTMENT_NOT_DELETABLE", ex.getErrorCode());
        assertEquals("DA_XAC_NHAN", app.getStatus()); // Không bị đổi sang DA_HUY
    }
}
