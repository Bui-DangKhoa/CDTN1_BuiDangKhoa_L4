package com.mekongmobile.smartcrm.controllers;

import com.mekongmobile.smartcrm.dto.ApiResponse;
import com.mekongmobile.smartcrm.dto.AppointmentResponseDto;
import com.mekongmobile.smartcrm.dto.CreateAppointmentRequest;
import com.mekongmobile.smartcrm.dto.TechnicianResponseDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;
import com.mekongmobile.smartcrm.dto.UpdateAppointmentRequest;
import com.mekongmobile.smartcrm.entities.Appointment;
import com.mekongmobile.smartcrm.exceptions.AppException;
import com.mekongmobile.smartcrm.services.AppointmentService;
import com.mekongmobile.smartcrm.services.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * TẦNG 1: Controller quản lý lịch hẹn Kỹ thuật viên (Appointment).
 * Điều hướng Web UI (Thymeleaf) và cung cấp REST API (QT-L4-01 / NFR2).
 *
 * @author MekongMobile CRM Team
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final TicketService ticketService;

    /**
     * MÀN HÌNH 3: Hiển thị form đặt lịch hẹn kỹ thuật viên.
     */
    @GetMapping("/appointments/new")
    public String showScheduleForm(
            @RequestParam(value = "ticketId", required = false) Long ticketId,
            Model model) {
        log.info("GET /appointments/new - Mở form đặt lịch hẹn cho ticketId: {}", ticketId);

        CreateAppointmentRequest request = new CreateAppointmentRequest();
        if (ticketId != null) {
            request.setTicketId(ticketId);
            loadTicketAndTechnicians(ticketId, model);
        }

        model.addAttribute("appointmentRequest", request);
        model.addAttribute("title", "Smart CRM - Đặt Lịch Hẹn Kỹ Thuật Viên");
        return "appointments/schedule-form";
    }

    /**
     * Tiếp nhận form submit đặt lịch hẹn từ Thymeleaf UI.
     */
    @PostMapping("/appointments")
    public String scheduleAppointment(
            @Valid @ModelAttribute("appointmentRequest") CreateAppointmentRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.info("POST /appointments - Tiếp nhận yêu cầu đặt lịch hẹn: {}", request);

        if (bindingResult.hasErrors()) {
            if (request.getTicketId() != null) {
                loadTicketAndTechnicians(request.getTicketId(), model);
            }
            return "appointments/schedule-form";
        }

        try {
            Appointment appointment = appointmentService.scheduleAppointment(
                    request.getTicketId(), request.getTechnicianId(), request.getAppointmentTime());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đặt lịch hẹn thành công cho phiếu #" + appointment.getTicket().getTicketCode()
                            + " với KTV " + appointment.getTechnician().getFullName()
                            + " vào lúc " + appointment.getAppointmentTime());

            return "redirect:/tickets/unassigned";
        } catch (AppException ex) {
            log.warn("Nghiệp vụ từ chối đặt lịch hẹn: {}", ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            if (request.getTicketId() != null) {
                loadTicketAndTechnicians(request.getTicketId(), model);
            }
            return "appointments/schedule-form";
        } catch (Exception ex) {
            log.error("Lỗi khi xử lý đặt lịch hẹn: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Lỗi xử lý hệ thống: " + ex.getMessage());
            if (request.getTicketId() != null) {
                loadTicketAndTechnicians(request.getTicketId(), model);
            }
            return "appointments/schedule-form";
        }
    }

    /**
     * REST API: Đặt lịch hẹn mới và kiểm tra chống trùng lịch (QT-L4-01 / NFR2).
     */
    @PostMapping("/api/v1/appointments")
    @ResponseBody
    public ResponseEntity<ApiResponse<AppointmentResponseDto>> scheduleAppointmentApi(
            @Valid @RequestBody CreateAppointmentRequest request) {
        log.info("REST API POST /api/v1/appointments - {}", request);
        Appointment appointment = appointmentService.scheduleAppointment(
                request.getTicketId(), request.getTechnicianId(), request.getAppointmentTime());

        AppointmentResponseDto responseDto = appointmentService.mapToDto(appointment);

        return new ResponseEntity<>(
                ApiResponse.of("Đặt lịch hẹn thành công (QT-L4-01)", responseDto),
                HttpStatus.CREATED
        );
    }

    /**
     * REST API: Lấy chi tiết lịch hẹn theo ID.
     */
    @GetMapping("/api/v1/appointments/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<AppointmentResponseDto>> getAppointmentByIdApi(@PathVariable("id") Long id) {
        log.info("REST API GET /api/v1/appointments/{}", id);
        AppointmentResponseDto dto = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(ApiResponse.of(dto));
    }

    /**
     * REST API: Lấy danh sách lịch hẹn (lọc theo ticketId hoặc technicianId nếu có).
     */
    @GetMapping("/api/v1/appointments")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<AppointmentResponseDto>>> getAllAppointmentsApi(
            @RequestParam(value = "ticketId", required = false) Long ticketId,
            @RequestParam(value = "technicianId", required = false) Long technicianId) {
        log.info("REST API GET /api/v1/appointments - ticketId: {}, technicianId: {}", ticketId, technicianId);
        List<AppointmentResponseDto> list = appointmentService.getAllAppointments(ticketId, technicianId);
        return ResponseEntity.ok(ApiResponse.of(list));
    }

    /**
     * REST API: Cập nhật lịch hẹn (Appointment PUT).
     * Kiểm tra chống trùng lịch QT-L4-01, loại trừ chính appointmentId đang cập nhật.
     */
    @PutMapping("/api/v1/appointments/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<AppointmentResponseDto>> updateAppointmentApi(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAppointmentRequest request) {
        log.info("REST API PUT /api/v1/appointments/{} - {}", id, request);
        AppointmentResponseDto updated = appointmentService.updateAppointment(id, request);
        return ResponseEntity.ok(ApiResponse.of("Cập nhật lịch hẹn thành công (QT-L4-01)", updated));
    }

    /**
     * REST API: Hủy lịch hẹn kỹ thuật viên (hủy mềm status = DA_HUY).
     * Chỉ cho phép khi chưa DA_XAC_NHAN, nếu không trả về HTTP 422 APPOINTMENT_NOT_DELETABLE.
     */
    @DeleteMapping("/api/v1/appointments/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> cancelAppointmentApi(@PathVariable("id") Long id) {
        log.info("REST API DELETE /api/v1/appointments/{} - Hủy lịch hẹn", id);
        appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(ApiResponse.of("Hủy lịch hẹn thành công", "Lịch hẹn đã được hủy mềm (DA_HUY)"));
    }

    private void loadTicketAndTechnicians(Long ticketId, Model model) {
        try {
            TicketResponseDto ticket = ticketService.getTicketById(ticketId);
            List<TechnicianResponseDto> eligibleTechs = ticketService.getEligibleTechniciansForTicket(ticketId);
            model.addAttribute("ticket", ticket);
            model.addAttribute("technicians", eligibleTechs);
        } catch (Exception ex) {
            log.warn("Không thể nạp thông tin ticket hoặc KTV cho ticketId {}: {}", ticketId, ex.getMessage());
        }
    }
}
