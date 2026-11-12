package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.entities.Ticket;
import com.mekongmobile.smartcrm.entities.TicketStatusLog;
import com.mekongmobile.smartcrm.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketDeleteAuditTest {

    @Mock TicketRepository ticketRepository;
    @Mock DeviceRepository deviceRepository;
    @Mock IssueCategoryRepository categoryRepository;
    @Mock TicketStatusLogRepository statusLogRepository;
    @Mock TechnicianRepository technicianRepository;
    @Mock AppointmentService appointmentService;
    @InjectMocks TicketServiceImpl ticketService;

    @Test
    void deleteServiceDoesNotExposeAnOverloadWithoutActorId() {
        assertThrows(NoSuchMethodException.class, () -> TicketService.class.getMethod("deleteTicket", Long.class));
    }

    @Test
    void softDeleteRecordsSessionActorAndKeepsTicketRowAndStatus() {
        long actorId = 42L;
        Ticket ticket = Ticket.builder().ticketId(10L).ticketCode("TK-10").centerId(1L).status("MOI").build();
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(appointmentService.hasActiveAppointment(10L)).thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(call -> call.getArgument(0));
        when(statusLogRepository.save(any(TicketStatusLog.class))).thenAnswer(call -> call.getArgument(0));

        ticketService.deleteTicket(10L, actorId);

        assertEquals("MOI", ticket.getStatus());
        assertNotNull(ticket.getDeletedAt());
        verify(ticketRepository).save(ticket);
        verify(ticketRepository, never()).delete(any(Ticket.class));
        ArgumentCaptor<TicketStatusLog> logCaptor = ArgumentCaptor.forClass(TicketStatusLog.class);
        verify(statusLogRepository).save(logCaptor.capture());
        assertEquals(actorId, logCaptor.getValue().getChangedBy());
        assertEquals("DA_XOA", logCaptor.getValue().getToStatus());
    }
}
