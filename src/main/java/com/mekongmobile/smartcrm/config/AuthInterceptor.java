package com.mekongmobile.smartcrm.config;

import com.mekongmobile.smartcrm.dto.UserSessionDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * TẦNG BẢO MẬT: Interceptor kiểm tra phiên đăng nhập và phân quyền theo 3 Actor:
 * 1. Public: Cho phép truy cập /login, /logout, /lookup/**, static assets tự do.
 * 2. QUAN_LY: Cho phép truy cập /tickets/**, /appointments/**.
 * 3. KY_THUAT_VIEN: Chỉ được truy cập /technician/**. Chặn truy cập URL Quản lý -> Trả lỗi 403 Forbidden.
 */
@Component
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = (contextPath != null && !contextPath.isEmpty()) ? uri.substring(contextPath.length()) : uri;

        // 1. Cho phép bỏ qua kiểm tra phiên với các URL công khai
        if (isPublicPath(path)) {
            return true;
        }

        // 2. Bắt buộc đăng nhập đối với các đường dẫn nội bộ
        HttpSession session = request.getSession(false);
        UserSessionDto currentUser = (session != null) ? (UserSessionDto) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            log.warn("Chưa đăng nhập! Chặn truy cập tới {} và chuyển hướng về /login", path);
            response.sendRedirect(contextPath + "/login");
            return false;
        }

        String role = currentUser.getRole();

        // 3. Phân quyền và chặn Kỹ thuật viên (KY_THUAT_VIEN) truy cập URL của Quản lý (/tickets/**, /appointments/**)
        if ("KY_THUAT_VIEN".equalsIgnoreCase(role)) {
            if (path.startsWith("/tickets") || path.startsWith("/appointments")) {
                log.warn("Kỹ thuật viên ({}) cố ý truy cập URL Quản lý {} -> Trả mã lỗi 403 Forbidden",
                        currentUser.getUsername(), path);
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Bạn không có quyền truy cập chức năng này (Quyền Quản lý yêu cầu).");
                return false;
            }
        }

        // 4. Nếu Quản lý truy cập nhầm trang cá nhân KTV thì chuyển hướng về trang điều phối
        if ("QUAN_LY".equalsIgnoreCase(role)) {
            if (path.startsWith("/technician")) {
                log.info("Quản lý ({}) truy cập trang KTV -> Điều hướng về /tickets/unassigned", currentUser.getUsername());
                response.sendRedirect(contextPath + "/tickets/unassigned");
                return false;
            }
        }

        return true;
    }

    private boolean isPublicPath(String path) {
        return path.equals("/") ||
                path.equals("/login") ||
                path.equals("/logout") ||
                path.startsWith("/lookup") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.startsWith("/webjars/") ||
                path.equals("/favicon.ico") ||
                path.startsWith("/error") ||
                path.startsWith("/swagger-ui") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/v3/api-docs");
    }
}
