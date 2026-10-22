package com.mekongmobile.smartcrm.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * TẦNG 1: Xử lý ngoại lệ tập trung toàn cục (Global Exception Handler).
 * Bắt các lỗi nghiệp vụ từ Tầng Service và chuyển thành ResponseErrorDto chuẩn.
 */
@RestControllerAdvice
@lombok.extern.slf4j.Slf4j
public class GlobalExceptionHandler {

    /**
     * Bắt lỗi nghiệp vụ AppException:
     * - NOT_FOUND -> 404 Not Found
     * - SCHEDULE_CONFLICT -> 409 Conflict
     * - KTV_NOT_QUALIFIED, INVALID_CHANGE_REASON, BAD_REQUEST -> 400 Bad Request
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ResponseErrorDto> handleAppException(AppException ex) {
        log.warn("Nghiệp vụ trả lỗi AppException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        HttpStatus status = resolveHttpStatus(ex);
        ResponseErrorDto errorDto = ResponseErrorDto.builder()
                .code(ex.getErrorCode() != null ? ex.getErrorCode() : "BAD_REQUEST")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorDto, status);
    }

    /**
     * Bắt lỗi nghiệp vụ BusinessRuleException kế thừa từ BT1.
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ResponseErrorDto> handleBusinessRuleException(BusinessRuleException ex) {
        log.warn("Nghiệp vụ trả lỗi BusinessRuleException: {}", ex.getMessage());
        ResponseErrorDto errorDto = ResponseErrorDto.builder()
                .code("BUSINESS_RULE_VIOLATION")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorDto, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Bắt lỗi không tìm thấy tài nguyên ResourceNotFoundException.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseErrorDto> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Không tìm thấy tài nguyên: {}", ex.getMessage());
        ResponseErrorDto errorDto = ResponseErrorDto.builder()
                .code("NOT_FOUND")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorDto, HttpStatus.NOT_FOUND);
    }

    /**
     * Bắt lỗi dữ liệu đầu vào không hợp lệ khi kiểm tra Bean Validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseErrorDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        StringBuilder sb = new StringBuilder();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            if (!sb.isEmpty()) {
                sb.append("; ");
            }
            sb.append(error.getField()).append(": ").append(error.getDefaultMessage());
        }

        String validationMsg = sb.isEmpty() ? "Dữ liệu đầu vào không hợp lệ" : sb.toString();
        log.warn("Dữ liệu đầu vào không hợp lệ (Bean Validation): {}", validationMsg);

        ResponseErrorDto errorDto = ResponseErrorDto.builder()
                .code("VALIDATION_ERROR")
                .message(validationMsg)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    /**
     * Bắt các ngoại lệ không xác định: Trả về HTTP 500 với thông báo thân thiện.
     * Bảo mật: Không bao giờ để lộ SQL error hoặc stack trace ra client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseErrorDto> handleGenericException(Exception ex) {
        log.error("Lỗi nội bộ hệ thống chưa được phân loại: ", ex);
        ResponseErrorDto errorDto = ResponseErrorDto.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message("Đã xảy ra lỗi nội bộ trong hệ thống. Vui lòng thử lại sau hoặc liên hệ quản trị viên.")
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private HttpStatus resolveHttpStatus(AppException ex) {
        if (ex.getStatus() != null && ex.getStatus() != HttpStatus.BAD_REQUEST) {
            return ex.getStatus();
        }
        if (ex.getErrorCode() == null) {
            return HttpStatus.BAD_REQUEST;
        }
        return switch (ex.getErrorCode().toUpperCase()) {
            case "NOT_FOUND", "RESOURCE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "SCHEDULE_CONFLICT", "CONFLICT" -> HttpStatus.CONFLICT;
            case "BUSINESS_RULE_VIOLATION" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "KTV_NOT_QUALIFIED", "INVALID_CHANGE_REASON", "BAD_REQUEST" -> HttpStatus.BAD_REQUEST;
            default -> ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_REQUEST;
        };
    }
}
