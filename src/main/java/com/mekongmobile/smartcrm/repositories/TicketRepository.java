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

    /**
     * MH3 / US4: Phiếu đang hoạt động của KTV (vd: status IN ('DA_PHAN_CONG','DANG_XU_LY')),
     * sắp xếp theo hạn SLA tăng dần. Tận dụng index idx_ticket_tech_due (technician_id, due_date).
     */
    List<Ticket> findByTechnician_TechnicianIdAndStatusInOrderByDueDateAsc(Long technicianId, java.util.Collection<String> statuses);

    List<Ticket> findAllByOrderByReceivedAtDesc();

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Ticket t WHERE t.technician IS NULL OR t.status = 'MOI' ORDER BY t.dueDate ASC")
    List<Ticket> findUnassignedTicketsOrderByDueDateAsc();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM Ticket t WHERE t.device.deviceId = :deviceId AND t.category.categoryId = :categoryId AND t.ticketId <> :ticketId")
    long countPreviousSameIssues(@org.springframework.data.repository.query.Param("deviceId") Long deviceId,
                                 @org.springframework.data.repository.query.Param("categoryId") Integer categoryId,
                                 @org.springframework.data.repository.query.Param("ticketId") Long ticketId);
}
