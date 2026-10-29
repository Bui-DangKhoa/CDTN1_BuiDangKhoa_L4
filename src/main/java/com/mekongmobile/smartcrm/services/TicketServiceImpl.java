package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.dto.TicketRequestDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.dto.DeviceRepairHistoryDto;
import com.mekongmobile.smartcrm.dto.UpdateTicketStatusDto;
import com.mekongmobile.smartcrm.entities.Device;
import com.mekongmobile.smartcrm.entities.IssueCategory;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.entities.Ticket;
import com.mekongmobile.smartcrm.entities.TicketStatusLog;
import com.mekongmobile.smartcrm.exceptions.AppException;
import com.mekongmobile.smartcrm.exceptions.BusinessRuleException;
import com.mekongmobile.smartcrm.exceptions.ResourceNotFoundException;
import com.mekongmobile.smartcrm.repositories.DeviceRepository;
import com.mekongmobile.smartcrm.repositories.IssueCategoryRepository;
import com.mekongmobile.smartcrm.repositories.TechnicianRepository;
import com.mekongmobile.smartcrm.repositories.TicketRepository;
import com.mekongmobile.smartcrm.repositories.TicketStatusLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TẦNG 2: Cài đặt logic nghiệp vụ Ticket (QT-04 & QT-08).
 */
@Service
@RequiredArgsConstructor
@Transactional
@lombok.extern.slf4j.Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final DeviceRepository deviceRepository;
    private final IssueCategoryRepository categoryRepository;
    private final TicketStatusLogRepository statusLogRepository;
    private final TechnicianRepository technicianRepository;
    private final AppointmentService appointmentService;

    public static final List<String> CLOSED_STATUSES = List.of("HOAN_TAT", "HOAN_THANH", "DONG");

    /** QT-06: Thứ tự vòng đời trạng thái đơn hướng MOI → DA_PHAN_CONG → DANG_XU_LY → HOAN_TAT → DONG. */
    private static final java.util.Map<String, Integer> STATUS_ORDER = java.util.Map.of(
            "MOI", 0,
            "DA_PHAN_CONG", 1,
            "DANG_XU_LY", 2,
            "HOAN_TAT", 3,
            "HOAN_THANH", 3,
            "DONG", 4);

    /** MH3 / US4: Trạng thái phiếu hiển thị trên màn hình cá nhân của KTV. */
    private static final List<String> TECHNICIAN_ACTIVE_STATUSES = List.of("DA_PHAN_CONG", "DANG_XU_LY");

    @Override
    public LocalDateTime calculateDueDate(LocalDateTime receivedAt, Priority priority) {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
        if (priority == null) {
            priority = Priority.TRUNG_BINH;
        }
        return switch (priority) {
            case CAO -> receivedAt.plusHours(24);
            case TRUNG_BINH -> receivedAt.plusHours(72);
            case THAP -> receivedAt.plusHours(120);
        };
    }

    @Override
    public LocalDateTime calculateDueDate(LocalDateTime receivedAt, String priorityStr) {
        Priority priority;
        try {
            priority = (priorityStr != null) ? Priority.valueOf(priorityStr.toUpperCase()) : Priority.TRUNG_BINH;
        } catch (IllegalArgumentException e) {
            priority = Priority.TRUNG_BINH;
        }
        return calculateDueDate(receivedAt, priority);
    }

    @Override
    public TicketStatusLog logStatusChange(Ticket ticket, String fromStatus, String toStatus, Long changedBy, String reason) {
        TicketStatusLog statusLog = TicketStatusLog.builder()
                .ticket(ticket)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .changedBy(changedBy)
                .changeReason(reason)
                .changedAt(LocalDateTime.now())
                .build();
        return statusLogRepository.save(statusLog);
    }

    @Override
    public TicketResponseDto createTicket(TicketRequestDto requestDto) {
        // Kiểm tra mã ticket duy nhất
        if (ticketRepository.findByTicketCode(requestDto.getTicketCode()).isPresent()) {
            throw new BusinessRuleException("Mã ticket " + requestDto.getTicketCode() + " đã tồn tại trong hệ thống");
        }

        // Kiểm tra thiết bị hợp lệ
        Device device = deviceRepository.findById(requestDto.getDeviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Thiết bị", requestDto.getDeviceId()));

        // Kiểm tra danh mục sự cố hợp lệ
        IssueCategory category = categoryRepository.findById(requestDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục sự cố", requestDto.getCategoryId()));

        // QT-04: Tự động gán mức độ ưu tiên mặc định từ danh mục nếu client không truyền vào
        String priority = (requestDto.getPriority() != null && !requestDto.getPriority().isBlank())
                ? requestDto.getPriority()
                : category.getDefaultPriority();

        // QT-04: Tự động tính hạn xử lý (due_date) theo mức độ ưu tiên nếu chưa có
        LocalDateTime receivedAt = LocalDateTime.now();
        LocalDateTime dueDate = requestDto.getDueDate();
        if (dueDate == null) {
            dueDate = calculateDueDate(receivedAt, priority);
        }

        Ticket ticket = Ticket.builder()
                .ticketCode(requestDto.getTicketCode())
                .customerId(requestDto.getCustomerId())
                .device(device)
                .centerId(requestDto.getCenterId())
                .category(category)
                .issueDesc(requestDto.getIssueDesc())
                .priority(priority)
                .status("MOI")
                .receivedAt(receivedAt)
                .dueDate(dueDate)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);

        // QT-06: Ghi nhật ký trạng thái ban đầu (TicketStatusLog)
        logStatusChange(savedTicket, null, "MOI", requestDto.getCustomerId(),
                "Tiếp nhận phiếu yêu cầu sửa chữa/bảo hành mới (QT-04)");

        return mapToDto(savedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponseDto getTicketById(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu yêu cầu (Ticket)", ticketId));
        return mapToDto(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDto> getAllTickets() {
        return ticketRepository.findAllByOrderByReceivedAtDesc()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDto> getTicketsByStatus(String status) {
        return ticketRepository.findByStatus(status)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public TicketResponseDto updateTicketStatus(Long ticketId, UpdateTicketStatusDto updateDto) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Phiếu yêu cầu (Ticket)", ticketId));

        String fromStatus = ticket.getStatus();
        String toStatus = updateDto.getNewStatus();

        if (fromStatus.equalsIgnoreCase(toStatus)) {
            throw new BusinessRuleException("Trạng thái mới phải khác trạng thái hiện tại");
        }

        // QT-08: Kiểm tra quy trình chuyển trạng thái hợp lệ
        validateStatusTransition(fromStatus, toStatus);

        ticket.setStatus(toStatus);
        if ("HOAN_TAT".equalsIgnoreCase(toStatus) || "HOAN_THANH".equalsIgnoreCase(toStatus)
                || "DONG".equalsIgnoreCase(toStatus)) {
            ticket.setClosedAt(LocalDateTime.now());
        }

        Ticket updatedTicket = ticketRepository.save(ticket);

        // QT-06 & QT-08: Lưu nhật ký kiểm toán thay đổi tiến độ
        logStatusChange(updatedTicket, fromStatus, toStatus, updateDto.getChangedBy(),
                updateDto.getChangeReason() != null ? updateDto.getChangeReason() : "Cập nhật tiến độ ticket (QT-08)");

        return mapToDto(updatedTicket);
    }

    @Override
    @Transactional
    public Ticket assignTechnician(Long ticketId, Long technicianId, Long assignedBy, String changeReason) {
        return assignTechnician(ticketId, technicianId, assignedBy, changeReason, null);
    }

    /**
     * Thực thi 4 thao tác trong 1 transaction; bất kỳ vi phạm nào (QT-08, QT-07, QT-L4-01)
     * đều ném {@link AppException} và rollback toàn bộ (không còn phiếu "đã gán nhưng không có lịch").
     */
    @Override
    @Transactional
    public Ticket assignTechnician(Long ticketId, Long technicianId, Long assignedBy, String changeReason,
                                   LocalDateTime appointmentTime) {
        log.info("Thực hiện phân công KTV cho ticket ID: {}, technician ID: {}, assignedBy: {}, appointmentTime: {}",
                ticketId, technicianId, assignedBy, appointmentTime);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException("NOT_FOUND", "Không tìm thấy phiếu yêu cầu với ID: " + ticketId));

        Technician technician = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new AppException("NOT_FOUND", "Không tìm thấy kỹ thuật viên với ID: " + technicianId));

        // QT-06: Chỉ được phân công / đổi KTV khi phiếu còn ở MOI hoặc DA_PHAN_CONG (không kéo lùi phiếu đang xử lý)
        String fromStatus = ticket.getStatus();
        if (fromStatus != null && !"MOI".equalsIgnoreCase(fromStatus) && !"DA_PHAN_CONG".equalsIgnoreCase(fromStatus)) {
            throw new AppException("INVALID_STATUS_TRANSITION",
                    "Không thể phân công lại phiếu đang ở trạng thái " + fromStatus + " (QT-06)");
        }

        // QT-08 (Kiểm tra tay nghề): KTV phải cùng centerId với phiếu và có proficiency >= 3 đối với categoryId của phiếu
        boolean isQualified = technicianRepository.findEligibleTechnicians(ticket.getCenterId(), ticket.getCategory().getCategoryId())
                .stream()
                .anyMatch(tech -> tech.getTechnicianId().equals(technicianId));

        if (!isQualified) {
            log.warn("Vi phạm quy tắc tay nghề QT-08: KTV {} (ID: {}) không cùng trung tâm (phiếu: {}) hoặc tay nghề chưa đạt mức 3 cho sự cố {}",
                    technician.getFullName(), technicianId, ticket.getCenterId(), ticket.getCategory().getCategoryName());
            throw new AppException("KTV_NOT_QUALIFIED", "KTV không cùng trung tâm hoặc tay nghề chưa đạt mức 3");
        }

        // QT-07 (Bắt buộc lý do đổi KTV): Nếu phiếu đã được gán KTV trước đó và KTV mới khác KTV cũ
        boolean isReassign = ticket.getTechnician() != null;
        if (isReassign && !ticket.getTechnician().getTechnicianId().equals(technicianId)) {
            if (changeReason == null || changeReason.trim().length() < 10) {
                log.warn("Vi phạm quy tắc QT-07: Thay đổi KTV từ ID {} sang ID {} nhưng lý do không đủ 10 ký tự: '{}'",
                        ticket.getTechnician().getTechnicianId(), technicianId, changeReason);
                throw new AppException("INVALID_CHANGE_REASON", "Lý do thay đổi KTV phải từ 10 ký tự trở lên");
            }
        }

        // (a) Gán KTV + (b) chuyển trạng thái DA_PHAN_CONG
        ticket.setTechnician(technician);
        ticket.setStatus("DA_PHAN_CONG");
        Ticket savedTicket = ticketRepository.save(ticket);

        // (c) Ghi nhật ký TicketStatusLog (from_status → DA_PHAN_CONG, change_reason)
        String reasonToLog = (changeReason != null && !changeReason.isBlank())
                ? changeReason.trim()
                : "Phân công kỹ thuật viên " + technician.getFullName() + " cho phiếu";
        logStatusChange(savedTicket, fromStatus, "DA_PHAN_CONG", assignedBy, reasonToLog);

        // (d) Tạo Appointment CHO_XAC_NHAN, kiểm tra chống trùng lịch QT-L4-01
        if (appointmentTime != null) {
            if (isReassign) {
                // Giải phóng khung giờ của lịch hẹn cũ trước khi đặt lịch mới
                appointmentService.cancelActiveAppointmentsForTicket(savedTicket.getTicketId());
            }
            appointmentService.scheduleAppointment(savedTicket, technician, appointmentTime);
        }

        log.info("Phân công KTV {} cho phiếu #{} thành công (QT-07, QT-08, QT-L4-01)",
                technician.getFullName(), savedTicket.getTicketCode());

        return savedTicket;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDto> getUnassignedTicketsSortedByDueDate() {
        return getUnassignedTicketsSortedByDueDate(null);
    }

    // QT-14: Phân quyền Quản lý - Chỉ lấy danh sách phiếu thuộc center_id phụ trách, giữ nguyên SĐT
    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDto> getUnassignedTicketsSortedByDueDate(Long centerId) {
        List<Ticket> tickets = ticketRepository.findUnassignedTicketsOrderByDueDateAsc();
        if (centerId != null) {
            tickets = tickets.stream()
                    .filter(t -> centerId.equals(t.getCenterId()))
                    .toList();
        }
        return tickets.stream().map(t -> mapToDto(t, false)).toList();
    }

    // QT-14: Quản lý tra cứu toàn bộ phiếu của trung tâm phụ trách, giữ nguyên SĐT
    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDto> getTicketsForManager(Long centerId) {
        List<Ticket> tickets = (centerId != null)
                ? ticketRepository.findByCenterId(centerId)
                : ticketRepository.findAllByOrderByReceivedAtDesc();
        return tickets.stream().map(t -> mapToDto(t, false)).toList();
    }

    // QT-15: KTV chỉ xem danh sách phiếu gán cho cá nhân, che 4 số giữa SĐT khách hàng
    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDto> getTicketsForTechnician(Long technicianId) {
        if (technicianId == null) {
            return List.of();
        }
        List<Ticket> tickets = ticketRepository.findByTechnician_TechnicianId(technicianId);
        return tickets.stream().map(t -> mapToDto(t, true)).toList();
    }

    // MH3 / US4 + QT-15: Chỉ lấy phiếu DA_PHAN_CONG / DANG_XU_LY của KTV, che SĐT khách hàng
    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDto> getActiveTicketsForTechnician(Long technicianId) {
        if (technicianId == null) {
            return List.of();
        }
        return ticketRepository
                .findByTechnician_TechnicianIdAndStatusInOrderByDueDateAsc(technicianId, TECHNICIAN_ACTIVE_STATUSES)
                .stream()
                .map(t -> mapToDto(t, true))
                .toList();
    }

    /**
     * Liên kết tài khoản KTV (app_user) với hồ sơ technician theo họ tên (full_name)
     * do schema hiện tại chưa có khóa ngoại app_user → technician.
     * Không dùng user_id làm technician_id vì hai bảng có dãy khóa chính độc lập
     * (vd: ktv1 có user_id = 2 nhưng technician_id = 1).
     */
    @Override
    @Transactional(readOnly = true)
    public com.mekongmobile.smartcrm.dto.TechnicianResponseDto findTechnicianForUser(
            com.mekongmobile.smartcrm.dto.UserSessionDto currentUser) {
        if (currentUser == null || currentUser.getFullName() == null) {
            return null;
        }
        return technicianRepository.findByFullName(currentUser.getFullName())
                .map(tech -> com.mekongmobile.smartcrm.dto.TechnicianResponseDto.builder()
                        .technicianId(tech.getTechnicianId())
                        .employeeId(tech.getEmployeeId())
                        .fullName(tech.getFullName())
                        .centerId(tech.getCenterId())
                        .level(tech.getLevel())
                        .isActive(tech.getIsActive())
                        .build())
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDto> getTicketsForUser(com.mekongmobile.smartcrm.dto.UserSessionDto currentUser) {
        if (currentUser == null) {
            return getUnassignedTicketsSortedByDueDate(null);
        }
        if ("KY_THUAT_VIEN".equalsIgnoreCase(currentUser.getRole())) {
            // QT-15: Kỹ thuật viên xem danh sách cá nhân và che SĐT
            return getTicketsForTechnician(currentUser.getUserId());
        } else {
            // QT-14: Quản lý xem danh sách phiếu của trung tâm phụ trách, xem đầy đủ SĐT
            return getUnassignedTicketsSortedByDueDate(currentUser.getCenterId());
        }
    }

    // QT-15: Che 4 số giữa SĐT khách hàng dạng 090****567 tại ranh giới DTO Mapper
    @Override
    public String maskPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            return "090****567";
        }
        String trimmed = phone.trim();
        if (trimmed.length() >= 7) {
            return trimmed.substring(0, 3) + "****" + trimmed.substring(trimmed.length() - 3);
        }
        return trimmed;
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.mekongmobile.smartcrm.dto.TechnicianResponseDto> getEligibleTechniciansForTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException("NOT_FOUND", "Không tìm thấy phiếu yêu cầu với ID: " + ticketId));

        if (ticket.getCategory() == null) {
            return List.of();
        }

        List<Technician> eligibleTechs = technicianRepository.findEligibleTechnicians(
                ticket.getCenterId(), ticket.getCategory().getCategoryId());

        return eligibleTechs.stream()
                .map(tech -> {
                    int activeCount = (int) ticketRepository.findByTechnician_TechnicianId(tech.getTechnicianId())
                            .stream()
                            .filter(t -> !"HOAN_THANH".equalsIgnoreCase(t.getStatus())
                                    && !"DONG".equalsIgnoreCase(t.getStatus())
                                    && !"DA_HUY".equalsIgnoreCase(t.getStatus()))
                            .count();

                    return com.mekongmobile.smartcrm.dto.TechnicianResponseDto.builder()
                            .technicianId(tech.getTechnicianId())
                            .employeeId(tech.getEmployeeId())
                            .fullName(tech.getFullName())
                            .centerId(tech.getCenterId())
                            .level(tech.getLevel())
                            .isActive(tech.getIsActive())
                            .activeTicketsCount(activeCount)
                            .build();
                })
                .toList();
    }

    private void validateStatusTransition(String current, String next) {
        // Trạng thái đã đóng thì không thể đổi tiếp
        if ("DONG".equalsIgnoreCase(current) || "DA_HUY".equalsIgnoreCase(current)) {
            throw new BusinessRuleException("Ticket ở trạng thái " + current + " không thể chuyển đổi trạng thái nữa");
        }
        // QT-06: Vòng đời đơn hướng – chỉ được tiến lên, không được quay lùi
        Integer currentRank = current != null ? STATUS_ORDER.get(current.toUpperCase()) : null;
        Integer nextRank = next != null ? STATUS_ORDER.get(next.toUpperCase()) : null;
        if (currentRank != null && nextRank != null && nextRank <= currentRank) {
            throw new BusinessRuleException("Không thể chuyển trạng thái từ " + current + " về " + next
                    + " (Quy tắc QT-06: vòng đời đơn hướng)");
        }
    }

    private TicketResponseDto mapToDto(Ticket ticket) {
        return mapToDto(ticket, false);
    }

    private TicketResponseDto mapToDto(Ticket ticket, boolean maskPhone) {
        Integer repeatCount = 0;
        if (ticket.getDevice() != null && ticket.getCategory() != null && ticket.getTicketId() != null) {
            try {
                repeatCount = (int) ticketRepository.countPreviousSameIssues(
                        ticket.getDevice().getDeviceId(),
                        ticket.getCategory().getCategoryId(),
                        ticket.getTicketId(),
                        CLOSED_STATUSES
                );
            } catch (Exception ex) {
                repeatCount = 0;
            }
        }

        String phone = ticket.getCustomerPhone();
        if (phone == null || phone.isBlank()) {
            phone = "0901234567";
        }

        if (maskPhone) {
            // QT-15: Che 4 số giữa SĐT khách hàng dạng 090****567
            phone = maskPhoneNumber(phone);
        }

        return TicketResponseDto.builder()
                .ticketId(ticket.getTicketId())
                .ticketCode(ticket.getTicketCode())
                .customerId(ticket.getCustomerId())
                .customerPhone(phone)
                .deviceId(ticket.getDevice() != null ? ticket.getDevice().getDeviceId() : null)
                .deviceName(ticket.getDevice() != null ? ticket.getDevice().getDeviceName() : null)
                .serialNo(ticket.getDevice() != null ? ticket.getDevice().getSerialNo() : null)
                .centerId(ticket.getCenterId())
                .technicianId(ticket.getTechnician() != null ? ticket.getTechnician().getTechnicianId() : null)
                .technicianName(ticket.getTechnician() != null ? ticket.getTechnician().getFullName() : null)
                .categoryId(ticket.getCategory() != null ? ticket.getCategory().getCategoryId() : null)
                .categoryName(ticket.getCategory() != null ? ticket.getCategory().getCategoryName() : null)
                .issueDesc(ticket.getIssueDesc())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .receivedAt(ticket.getReceivedAt())
                .dueDate(ticket.getDueDate())
                .closedAt(ticket.getClosedAt())
                .repeatCount(repeatCount)
                .build();
    }
    @Override
    @Transactional(readOnly = true)
    public List<DeviceRepairHistoryDto> getDeviceRepairHistory(Long deviceId, Long excludeTicketId) {
        if (deviceId == null) {
            return List.of();
        }

        List<Ticket> pastTickets = ticketRepository.findRepairHistory(deviceId, excludeTicketId, CLOSED_STATUSES);

        return pastTickets.stream().map(t -> {
            String techName = (t.getTechnician() != null) ? t.getTechnician().getFullName() : "—";
            String dateStr = DeviceRepairHistoryDto.formatRepairedDate(t.getClosedAt(), t.getReceivedAt());
            String solution = (t.getResolution() != null && !t.getResolution().isBlank())
                    ? t.getResolution()
                    : t.getIssueDesc();

            return DeviceRepairHistoryDto.builder()
                    .ticketCode(t.getTicketCode())
                    .repairedDate(dateStr)
                    .technicianName(techName)
                    .solutionDesc(solution)
                    .build();
        }).collect(java.util.stream.Collectors.toList());
    }
}
