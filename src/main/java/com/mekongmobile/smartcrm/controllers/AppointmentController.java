package com.mekongmobile.smartcrm.controllers;

import com.mekongmobile.smartcrm.dto.ApiResponse;
import com.mekongmobile.smartcrm.dto.AppointmentResponseDto;
import com.mekongmobile.smartcrm.dto.CreateAppointmentRequest;
import com.mekongmobile.smartcrm.dto.TechnicianResponseDto;
import com.mekongmobile.smartcrm.dto.TicketResponseDto;
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

        AppointmentResponseDto responseDto = AppointmentResponseDto.builder()
                .appointmentId(appointment.getAppointmentId())
                .ticketId(appointment.getTicket().getTicketId())
                .ticketCode(appointment.getTicket().getTicketCode())
                .technicianId(appointment.getTechnician().getTechnicianId())
                .technicianName(appointment.getTechnician().getFullName())
                .appointmentTime(appointment.getAppointmentTime())
                .status(appointment.getStatus())
                .createdAt(appointment.getCreatedAt())
                .build();

        return new ResponseEntity<>(
                ApiResponse.of("Đặt lịch hẹn thành công (QT-L4-01)", responseDto),
                HttpStatus.CREATED
        );
    }

    /**
     * REST API: Hủy lịch hẹn kỹ thuật viên.
     */
    @DeleteMapping("/api/v1/appointments/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<String>> cancelAppointmentApi(@PathVariable("id") Long id) {
        log.info("REST API DELETE /api/v1/appointments/{} - Hủy lịch hẹn", id);
        appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(ApiResponse.of("Hủy lịch hẹn thành công", "Lịch hẹn đã được hủy"));
    }

    private void loadTicketAndTechnicians(Long ticketId, Model model) {
        try {
            TicketResponseDto ticket = ticketService.getTicketById(ticketId);
            List<TechnicianResponseDto> eligibleTechs = ticketService.getEligibleTechniciansForTicket(ticketId);
            model.addAttribute("ticket", ticket);
            model.addAttribute("technicians", eligibleTechs);
        } catch (Exception ignored) {
        }
    }
}
