package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.entities.Ticket;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.exceptions.AppException;
import com.mekongmobile.smartcrm.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketAssignmentStatusGuardTest {

    @Mock TicketRepository ticketRepository;
    @Mock DeviceRepository deviceRepository;
    @Mock IssueCategoryRepository categoryRepository;
    @Mock TicketStatusLogRepository statusLogRepository;
    @Mock TechnicianRepository technicianRepository;
    @Mock AppointmentService appointmentService;
    @InjectMocks TicketServiceImpl ticketService;

    @Test
    void assigningCompletedTicketIsRejectedWithoutChangesOrLog() {
        assertAssignmentRejected("HOAN_TAT");
    }

    @Test
    void assigningClosedTicketIsRejectedWithoutChangesOrLog() {
        assertAssignmentRejected("DA_DONG");
    }

    private void assertAssignmentRejected(String initialStatus) {
        Ticket ticket = Ticket.builder().ticketId(44L).centerId(1L).status(initialStatus).build();
        ticket.setStatus(initialStatus);
        when(ticketRepository.findById(44L)).thenReturn(Optional.of(ticket));
        when(technicianRepository.findById(7L)).thenReturn(Optional.of(Technician.builder().technicianId(7L).build()));

        AppException ex = assertThrows(AppException.class,
                () -> ticketService.assignTechnician(44L, 7L, 2L, "Phân công KTV", null, 1L));

        assertEquals("INVALID_STATUS_TRANSITION", ex.getErrorCode());
        assertEquals(initialStatus, ticket.getStatus());
        assertNull(ticket.getTechnician());
        verify(ticketRepository, never()).save(any());
        verify(statusLogRepository, never()).save(any());
    }
}
