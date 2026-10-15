package com.mekongmobile.smartcrm.repositories;

import com.mekongmobile.smartcrm.entities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TẦNG 3: Repository quản lý phiếu yêu cầu dịch vụ (Ticket).
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByStatus(String status);

    List<Ticket> findByCenterId(Long centerId);

    List<Ticket> findByTechnician_TechnicianId(Long technicianId);

    List<Ticket> findAllByOrderByReceivedAtDesc();
}
