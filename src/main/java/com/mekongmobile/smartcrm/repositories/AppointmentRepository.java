package com.mekongmobile.smartcrm.repositories;

import com.mekongmobile.smartcrm.entities.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TẦNG 3: Repository quản lý lịch hẹn kỹ thuật viên (Appointment).
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByTicket_TicketId(Long ticketId);

    List<Appointment> findByTechnician_TechnicianId(Long technicianId);

    /**
     * QT-L4-01 / NFR2: Kiểm tra KTV đã có lịch hẹn còn hiệu lực (status <> statusNot)
     * tại thời điểm appointmentTime bằng derived query method của Spring Data JPA.
     */
    boolean existsByTechnician_TechnicianIdAndAppointmentTimeAndStatusNot(
            Long technicianId, LocalDateTime appointmentTime, String status);

    /**
     * QT-L4-01 / NFR2: Kiểm tra KTV đã có lịch hẹn còn hiệu lực (status &lt;&gt; 'DA_HUY')
     * tại đúng thời điểm {@code appointmentTime} hay chưa.
     * Tương ứng UNIQUE INDEX (technician_id, active_tech_time) ở CSDL.
     */
    @Query("""
        SELECT COUNT(a) > 0 FROM Appointment a
        WHERE a.technician.technicianId = :technicianId
          AND a.appointmentTime = :appointmentTime
          AND a.status <> 'DA_HUY'
    """)
    boolean existsActiveAppointmentAtTime(@Param("technicianId") Long technicianId, 
                                          @Param("appointmentTime") LocalDateTime appointmentTime);

    /**
     * QT-L4-01 / NFR2: Kiểm tra KTV có lịch hẹn còn hiệu lực (loại trừ 'DA_HUY')
     * rơi vào khung giờ [windowStart, windowEnd) hay không.
     */
    @Query("""
        SELECT COUNT(a) > 0 FROM Appointment a
        WHERE a.technician.technicianId = :technicianId
          AND a.appointmentTime >= :windowStart
          AND a.appointmentTime < :windowEnd
          AND a.status <> 'DA_HUY'
    """)
    boolean existsActiveAppointmentInWindow(@Param("technicianId") Long technicianId,
                                            @Param("windowStart") LocalDateTime windowStart,
                                            @Param("windowEnd") LocalDateTime windowEnd);

    List<Appointment> findByTechnician_TechnicianIdAndAppointmentTimeBetween(
            Long technicianId, LocalDateTime start, LocalDateTime end);

    /**
     * US7 / FR7: Tìm kiếm lịch hẹn công khai theo mã phiếu (ticketCode) hoặc SĐT khách hàng.
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.ticket.ticketCode = :keyword
           OR a.ticket.customerPhone = :keyword
        ORDER BY a.appointmentTime DESC
    """)
    List<Appointment> findByTicketCodeOrPhone(@Param("keyword") String keyword);
}
