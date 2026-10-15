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
     * Kiểm tra kỹ thuật viên đã có lịch hẹn trùng giờ mà chưa bị hủy hay không.
     */
    @Query("""
        SELECT COUNT(a) > 0 FROM Appointment a
        WHERE a.technician.technicianId = :technicianId
          AND a.appointmentTime = :appointmentTime
          AND a.status <> 'DA_HUY'
    """)
    boolean existsActiveAppointmentAtTime(@Param("technicianId") Long technicianId, 
                                          @Param("appointmentTime") LocalDateTime appointmentTime);

    List<Appointment> findByTechnician_TechnicianIdAndAppointmentTimeBetween(
            Long technicianId, LocalDateTime start, LocalDateTime end);
}
