package com.mekongmobile.smartcrm.config;

import com.mekongmobile.smartcrm.exceptions.AppException;
import com.mekongmobile.smartcrm.exceptions.GlobalExceptionHandler;
import com.mekongmobile.smartcrm.exceptions.ResponseErrorDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import jakarta.servlet.ServletException;

import static org.junit.jupiter.api.Assertions.*;

class LoggingAndMaskingTest {

    @Test
    @DisplayName("QT-15: Che 4 số giữa SĐT khách hàng (090****567)")
    void testMaskPhoneNumber_QT15() {
        String original = "Khách hàng có số điện thoại 0901234567 yêu cầu hỗ trợ";
        String masked = MaskingUtil.maskPhoneNumber(original);

        assertTrue(masked.contains("090****567"));
        assertFalse(masked.contains("0901234567"));
    }

    @Test
    @DisplayName("Bảo mật: Che mật khẩu và token trong chuỗi log")
    void testMaskSensitiveData() {
        String logMessage = "Người dùng đăng nhập password: 'supersecret' và token: abcdef123456";
        String masked = MaskingUtil.maskSensitiveData(logMessage);

        assertFalse(masked.contains("supersecret"));
        assertFalse(masked.contains("abcdef123456"));
        assertTrue(masked.contains("******"));
    }

    @Test
    @DisplayName("RequestIdFilter: Tự động sinh X-Request-Id UUID khi client không truyền")
    void testRequestIdFilterGeneratesUuid() throws ServletException, IOException {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        String requestId = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        assertNotNull(requestId);
        assertFalse(requestId.isBlank());
        // MDC phải được giải phóng sau filter
        assertNull(MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY));
    }

    @Test
    @DisplayName("RequestIdFilter: Giữ nguyên X-Request-Id khi client đã truyền lên")
    void testRequestIdFilterPreservesExistingId() throws ServletException, IOException {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "custom-request-id-999");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals("custom-request-id-999", response.getHeader(RequestIdFilter.REQUEST_ID_HEADER));
    }

    @Test
    @DisplayName("GlobalExceptionHandler: Bắt AppException trả về HTTP 4xx và JSON { error, message }")
    void testHandleAppException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        AppException ex = new AppException("SCHEDULE_CONFLICT", "KTV đã có lịch hẹn khác trong khung giờ này");

        ResponseEntity<ResponseErrorDto> response = handler.handleAppException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SCHEDULE_CONFLICT", response.getBody().getError());
        assertEquals("KTV đã có lịch hẹn khác trong khung giờ này", response.getBody().getMessage());
        assertNotNull(response.getBody().getRequestId());
    }

    @Test
    @DisplayName("GlobalExceptionHandler: Ánh xạ mã lỗi TICKET_NOT_FOUND và APPOINTMENT_NOT_FOUND sang HTTP 404")
    void testResolveHttpStatus_notFoundErrors() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        
        ResponseEntity<ResponseErrorDto> resp1 = handler.handleAppException(new AppException("TICKET_NOT_FOUND", "Không tìm thấy phiếu"));
        assertEquals(HttpStatus.NOT_FOUND, resp1.getStatusCode());
        assertEquals("TICKET_NOT_FOUND", resp1.getBody().getError());

        ResponseEntity<ResponseErrorDto> resp2 = handler.handleAppException(new AppException("APPOINTMENT_NOT_FOUND", "Không tìm thấy lịch hẹn"));
        assertEquals(HttpStatus.NOT_FOUND, resp2.getStatusCode());
        assertEquals("APPOINTMENT_NOT_FOUND", resp2.getBody().getError());
    }

    @Test
    @DisplayName("GlobalExceptionHandler: Ánh xạ mã lỗi TICKET_NOT_DELETABLE và APPOINTMENT_NOT_DELETABLE sang HTTP 422")
    void testResolveHttpStatus_notDeletableErrors() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ResponseErrorDto> resp1 = handler.handleAppException(new AppException("TICKET_NOT_DELETABLE", "Không thể xóa phiếu"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp1.getStatusCode());
        assertEquals("TICKET_NOT_DELETABLE", resp1.getBody().getError());

        ResponseEntity<ResponseErrorDto> resp2 = handler.handleAppException(new AppException("APPOINTMENT_NOT_DELETABLE", "Không thể hủy lịch"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp2.getStatusCode());
        assertEquals("APPOINTMENT_NOT_DELETABLE", resp2.getBody().getError());
    }

    @Test
    @DisplayName("GlobalExceptionHandler: Bắt Exception trả về HTTP 500 kèm mã tra cứu và không lộ chi tiết SQL")
    void testHandleGenericException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Exception ex = new RuntimeException("Syntax error in SQL statement: SELECT * FROM secret_table");

        ResponseEntity<ResponseErrorDto> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("LOI_HE_THONG", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("Mã tra cứu:"));
        assertFalse(response.getBody().getMessage().contains("SELECT * FROM"));
    }
}
