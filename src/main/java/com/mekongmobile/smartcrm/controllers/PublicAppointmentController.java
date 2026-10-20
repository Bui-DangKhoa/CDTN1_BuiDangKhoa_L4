package com.mekongmobile.smartcrm.controllers;

import com.mekongmobile.smartcrm.entities.Appointment;
import com.mekongmobile.smartcrm.exceptions.AppException;
import com.mekongmobile.smartcrm.services.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * TẦNG 1: Controller Cổng Tra cứu Công khai dành cho Khách hàng (US7/FR7).
 * Cho phép khách hàng tra cứu tiến độ/lịch hẹn bảo hành bằng Mã phiếu hoặc SĐT mà không cần đăng nhập.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PublicAppointmentController {

    private final AppointmentService appointmentService;

    /**
     * US7 / FR7: Màn hình tra cứu thông tin lịch hẹn công khai.
     * Tiếp nhận mã phiếu bảo hành hoặc số điện thoại khách hàng.
     */
    @GetMapping("/lookup")
    public String showLookupPortal(
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model) {

        log.info("GET /lookup - Tra cứu công khai với từ khóa: {}", keyword);

        if (keyword != null && !keyword.trim().isEmpty()) {
            String cleanKeyword = keyword.trim();
            List<Appointment> appointments = appointmentService.findAppointmentsByKeyword(cleanKeyword);

            model.addAttribute("keyword", cleanKeyword);
            model.addAttribute("appointments", appointments);
            model.addAttribute("searched", true);
        } else {
            model.addAttribute("searched", false);
        }

        model.addAttribute("title", "Tra cứu Lịch hẹn Bảo hành - Mekong Mobile");
        return "public/appointment-lookup";
    }

    /**
     * US7 / FR7: Khách hàng nhấn nút "Xác nhận lịch hẹn", cập nhật trạng thái sang DA_XAC_NHAN.
     */
    @PostMapping("/lookup/confirm/{appointmentId}")
    public String confirmAppointment(
            @PathVariable("appointmentId") Long appointmentId,
            @RequestParam(value = "keyword", required = false) String keyword,
            RedirectAttributes redirectAttributes) {

        log.info("POST /lookup/confirm/{} - Khách hàng xác nhận lịch hẹn", appointmentId);

        try {
            Appointment appointment = appointmentService.confirmAppointment(appointmentId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Xác nhận lịch hẹn thành công! Kỹ thuật viên sẽ tiếp nhận hỗ trợ đúng thời gian đã hẹn.");

            String returnKeyword = (keyword != null && !keyword.isBlank())
                    ? keyword
                    : (appointment.getTicket() != null ? appointment.getTicket().getTicketCode() : "");

            return "redirect:/lookup?keyword=" + returnKeyword;
        } catch (AppException ex) {
            log.warn("Lỗi khi xác nhận lịch hẹn id {}: {}", appointmentId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/lookup" + (keyword != null ? "?keyword=" + keyword : "");
        }
    }
}
