package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.dto.TicketRequestDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.dto.UpdateTicketStatusDto;
import com.mekongmobile.smartcrm.entities.Device;
import com.mekongmobile.smartcrm.entities.IssueCategory;
import com.mekongmobile.smartcrm.entities.Ticket;
import com.mekongmobile.smartcrm.entities.TicketStatusLog;
import com.mekongmobile.smartcrm.exceptions.BusinessRuleException;
import com.mekongmobile.smartcrm.exceptions.ResourceNotFoundException;
import com.mekongmobile.smartcrm.repositories.DeviceRepository;
import com.mekongmobile.smartcrm.repositories.IssueCategoryRepository;
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
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final DeviceRepository deviceRepository;
    private final IssueCategoryRepository categoryRepository;
    private final TicketStatusLogRepository statusLogRepository;

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
            dueDate = switch (priority.toUpperCase()) {
                case "KHAN_CAP", "CAO" -> receivedAt.plusHours(24);
                case "TRUNG_BINH" -> receivedAt.plusHours(48);
                default -> receivedAt.plusHours(72);
            };
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

        // Ghi nhật ký trạng thái ban đầu (TicketStatusLog)
        TicketStatusLog initialLog = TicketStatusLog.builder()
                .ticket(savedTicket)
                .fromStatus(null)
                .toStatus("MOI")
                .changedBy(requestDto.getCustomerId())
                .changeReason("Tiếp nhận phiếu yêu cầu sửa chữa/bảo hành mới (QT-04)")
                .changedAt(receivedAt)
                .build();
        statusLogRepository.save(initialLog);

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
        if ("HOAN_THANH".equalsIgnoreCase(toStatus) || "DONG".equalsIgnoreCase(toStatus)) {
            ticket.setClosedAt(LocalDateTime.now());
        }

        Ticket updatedTicket = ticketRepository.save(ticket);

        // QT-08: Lưu nhật ký kiểm toán thay đổi tiến độ
        TicketStatusLog statusLog = TicketStatusLog.builder()
                .ticket(updatedTicket)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .changedBy(updateDto.getChangedBy())
                .changeReason(updateDto.getChangeReason() != null ? updateDto.getChangeReason() : "Cập nhật tiến độ ticket (QT-08)")
                .changedAt(LocalDateTime.now())
                .build();
        statusLogRepository.save(statusLog);

        return mapToDto(updatedTicket);
    }

    private void validateStatusTransition(String current, String next) {
        // Trạng thái đã đóng thì không thể đổi tiếp
        if ("DONG".equalsIgnoreCase(current) || "DA_HUY".equalsIgnoreCase(current)) {
            throw new BusinessRuleException("Ticket ở trạng thái " + current + " không thể chuyển đổi trạng thái nữa");
        }
    }

    private TicketResponseDto mapToDto(Ticket ticket) {
        return TicketResponseDto.builder()
                .ticketId(ticket.getTicketId())
                .ticketCode(ticket.getTicketCode())
                .customerId(ticket.getCustomerId())
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
                .build();
    }
}
