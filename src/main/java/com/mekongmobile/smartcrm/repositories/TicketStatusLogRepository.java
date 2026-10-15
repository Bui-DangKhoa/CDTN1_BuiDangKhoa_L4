package com.mekongmobile.smartcrm.repositories;

import com.mekongmobile.smartcrm.entities.TicketStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TẦNG 3: Repository quản lý lịch sử chuyển đổi trạng thái ticket (TicketStatusLog).
 */
@Repository
public interface TicketStatusLogRepository extends JpaRepository<TicketStatusLog, Long> {

    List<TicketStatusLog> findByTicket_TicketIdOrderByChangedAtDesc(Long ticketId);
}
