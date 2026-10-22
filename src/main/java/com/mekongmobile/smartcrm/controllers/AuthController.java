package com.mekongmobile.smartcrm.controllers;

import com.mekongmobile.smartcrm.dto.LoginRequest;
import com.mekongmobile.smartcrm.dto.UserSessionDto;
import com.mekongmobile.smartcrm.exceptions.AppException;
import com.mekongmobile.smartcrm.services.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * TẦNG 1: Controller quản lý Đăng nhập & Đăng xuất cho Nhân viên nội bộ.
 * Lưu trữ thông tin UserSessionDto vào HttpSession và điều hướng theo vai trò (Role).
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Hiển thị trang đăng nhập.
     * Nếu đã đăng nhập thì tự động chuyển hướng theo vai trò.
     */
    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(value = "logout", required = false) String logout,
            HttpSession session,
            Model model) {

        if (logout != null) {
            if (session != null) {
                session.invalidate();
            }
            model.addAttribute("infoMessage", "Bạn đã đăng xuất khỏi hệ thống thành công.");
        } else {
            UserSessionDto currentUser = (session != null) ? (UserSessionDto) session.getAttribute("currentUser") : null;
            if (currentUser != null) {
                model.addAttribute("currentLoggedInUser", currentUser);
            }
        }

        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest());
        }

        model.addAttribute("title", "Đăng nhập - Smart CRM Mekong Mobile");
        return "auth/login";
    }

    /**
     * Xử lý xác thực đăng nhập từ người dùng.
     */
    @PostMapping("/login")
    public String processLogin(
            @Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.info("POST /login - Yêu cầu đăng nhập từ tài khoản: {}", loginRequest.getUsername());

        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "Đăng nhập - Smart CRM Mekong Mobile");
            return "auth/login";
        }

        try {
            UserSessionDto userSession = authService.login(loginRequest);
            session.setAttribute("currentUser", userSession);

            // Điều hướng đăng nhập theo đúng vai trò Actor
            if ("QUAN_LY".equalsIgnoreCase(userSession.getRole())) {
                return "redirect:/tickets/unassigned";
            } else {
                return "redirect:/technician/my-tickets";
            }
        } catch (AppException ex) {
            log.warn("Đăng nhập thất bại: {}", ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("title", "Đăng nhập - Smart CRM Mekong Mobile");
            return "auth/login";
        }
    }

    /**
     * Xóa session và đăng xuất người dùng khỏi hệ thống.
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        log.info("GET /logout - Người dùng đăng xuất khỏi hệ thống");
        session.invalidate();
        return "redirect:/login?logout=true";
    }
}
