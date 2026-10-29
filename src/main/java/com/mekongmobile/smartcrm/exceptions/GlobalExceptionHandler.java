package com.mekongmobile.smartcrm.exceptions;

import com.mekongmobile.smartcrm.config.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;

/**
 * Xử lý ngoại lệ tập trung toàn cục (Global Exception Handler).
 * - Bắt AppException (lỗi nghiệp vụ): Ghi log mức INFO kèm requestId, trả về HTTP 4xx { "error": "MA_LOI", "message": "Thông báo" }.
 * - Bắt Exception (lỗi hệ thống/CSDL): Ghi log mức ERROR kèm requestId và stack trace;
 *   + Nếu request HTML (Accept: text/html và không phải /api/**): trả về view Thymeleaf error/500 có requestId, banner đỏ, nút Thử lại, không lộ thông tin kỹ thuật.
 *   + Nếu request API / JSON: trả về HTTP 500 kèm mã tra cứu: { "error": "LOI_HE_THONG", "message": "Hệ thống đang gặp sự cố. Mã tra cứu: <requestId>" }.
 * Tuyệt đối không để lộ câu lệnh SQL hay stack trace ra client.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Bắt lỗi nghiệp vụ AppException: Ghi log INFO kèm requestId, trả về HTTP 4xx
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ResponseErrorDto> handleAppException(AppException ex) {
        String requestId = RequestIdFilter.getCurrentRequestId();
        String errorCode = ex.getErrorCode() != null ? ex.getErrorCode() : "BAD_REQUEST";
        HttpStatus status = resolveHttpStatus(ex);

        log.info("[requestId: {}] Lỗi nghiệp vụ AppException [{}]: {}", requestId, errorCode, ex.getMessage());

        ResponseErrorDto errorDto = ResponseErrorDto.builder()
                .error(errorCode)
                .message(ex.getMessage())
                .requestId(requestId)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorDto, status);
    }

    /**
     * Bắt lỗi quy tắc nghiệp vụ BusinessRuleException (HTTP 422)
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ResponseErrorDto> handleBusinessRuleException(BusinessRuleException ex) {
        String requestId = RequestIdFilter.getCurrentRequestId();
        log.info("[requestId: {}] Vi phạm quy tắc nghiệp vụ BusinessRuleException: {}", requestId, ex.getMessage());

        ResponseErrorDto errorDto = ResponseErrorDto.builder()
                .error("BUSINESS_RULE_VIOLATION")
                .message(ex.getMessage())
                .requestId(requestId)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorDto, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Bắt lỗi không tìm thấy tài nguyên ResourceNotFoundException (HTTP 404)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseErrorDto> handleResourceNotFoundException(ResourceNotFoundException ex) {
        String requestId = RequestIdFilter.getCurrentRequestId();
        log.info("[requestId: {}] Không tìm thấy tài nguyên: {}", requestId, ex.getMessage());

        ResponseErrorDto errorDto = ResponseErrorDto.builder()
                .error("NOT_FOUND")
                .message(ex.getMessage())
                .requestId(requestId)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorDto, HttpStatus.NOT_FOUND);
    }

    /**
     * Bắt lỗi dữ liệu đầu vào Bean Validation (HTTP 400)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseErrorDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String requestId = RequestIdFilter.getCurrentRequestId();
        StringBuilder sb = new StringBuilder();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            if (!sb.isEmpty()) {
                sb.append("; ");
            }
            sb.append(error.getField()).append(": ").append(error.getDefaultMessage());
        }

        String validationMsg = sb.isEmpty() ? "Dữ liệu đầu vào không hợp lệ" : sb.toString();
        log.info("[requestId: {}] Dữ liệu đầu vào không hợp lệ (Validation): {}", requestId, validationMsg);

        ResponseErrorDto errorDto = ResponseErrorDto.builder()
                .error("VALIDATION_ERROR")
                .message(validationMsg)
                .requestId(requestId)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    /**
     * Bắt mọi ngoại lệ không xác định (lỗi hệ thống / CSDL):
     * Ghi log ERROR kèm requestId và stack trace.
     * - Nếu request HTML (Accept: text/html và không phải /api/**): Trả về ModelAndView "error/500".
     * - Nếu request API hoặc JSON: Trả về HTTP 500 kèm JSON { "error": "LOI_HE_THONG", ... }.
     * Tuyệt đối không để lộ SQL query hay stack trace ra client.
     */
    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request) {
        String requestId = RequestIdFilter.getCurrentRequestId();
        log.error("[requestId: {}] Lỗi hệ thống/CSDL chưa được phân loại: {}", requestId, ex.getMessage(), ex);

        boolean isHtmlRequest = false;
        if (request != null) {
            String uri = request.getRequestURI();
            String accept = request.getHeader("Accept");
            boolean isApi = uri != null && uri.startsWith("/api/");
            if (!isApi && accept != null && accept.contains(MediaType.TEXT_HTML_VALUE)) {
                isHtmlRequest = true;
            }
        }

        if (isHtmlRequest) {
            ModelAndView mav = new ModelAndView("error/500");
            mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            mav.addObject("requestId", requestId);
            return mav;
        }

        ResponseErrorDto errorDto = ResponseErrorDto.builder()
                .error("LOI_HE_THONG")
                .message("Hệ thống đang gặp sự cố. Mã tra cứu: " + requestId)
                .requestId(requestId)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Helper overload cho unit test hoặc các lời gọi không truyền HttpServletRequest.
     */
    public ResponseEntity<ResponseErrorDto> handleGenericException(Exception ex) {
        return (ResponseEntity<ResponseErrorDto>) handleGenericException(ex, (HttpServletRequest) null);
    }

    private HttpStatus resolveHttpStatus(AppException ex) {
        String errorCode = ex.getErrorCode();
        if (errorCode == null) {
            return HttpStatus.BAD_REQUEST;
        }
        return switch (errorCode.toUpperCase()) {
            case "NOT_FOUND", "RESOURCE_NOT_FOUND", "TICKET_NOT_FOUND", "APPOINTMENT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "SCHEDULE_CONFLICT", "CONFLICT" -> HttpStatus.CONFLICT;
            case "BUSINESS_RULE_VIOLATION", "TICKET_NOT_DELETABLE", "APPOINTMENT_NOT_DELETABLE" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "UNAUTHORIZED", "INVALID_CREDENTIALS" -> HttpStatus.UNAUTHORIZED;
            case "FORBIDDEN", "ACCESS_DENIED" -> HttpStatus.FORBIDDEN;
            case "KTV_NOT_QUALIFIED", "INVALID_CHANGE_REASON", "INVALID_STATUS_TRANSITION",
                 "INVALID_APPOINTMENT_TIME", "BAD_REQUEST", "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
