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

    /**
     * Hàng đợi điều phối: phiếu theo trạng thái của một trung tâm, ưu tiên hạn SLA (due_date) gần nhất.
     * Tận dụng index idx_ticket_status_due (status, due_date).
     */
    List<Ticket> findByStatusAndCenterIdOrderByDueDateAsc(String status, Long centerId);

    List<Ticket> findByTechnician_TechnicianId(Long technicianId);

    List<Ticket> findAllByOrderByReceivedAtDesc();
}
