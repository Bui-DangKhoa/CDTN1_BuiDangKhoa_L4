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
public class GlobalExceptionHandler {

    /**
     * Bắt lỗi nghiệp vụ AppException:
     * - NOT_FOUND -> 404 Not Found
     * - SCHEDULE_CONFLICT -> 409 Conflict
     * - KTV_NOT_QUALIFIED, INVALID_CHANGE_REASON, BAD_REQUEST -> 400 Bad Request
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ResponseErrorDto> handleAppException(AppException ex) {
        HttpStatus status = resolveHttpStatus(ex);
        ResponseErrorDto errorDto = ResponseErrorDto.builder()
                .code(ex.getErrorCode() != null ? ex.getErrorCode() : "BAD_REQUEST")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorDto, status);
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

        ResponseErrorDto errorDto = ResponseErrorDto.builder()
                .code("VALIDATION_ERROR")
                .message(sb.isEmpty() ? "Dữ liệu đầu vào không hợp lệ" : sb.toString())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    /**
     * Bắt các ngoại lệ không xác định: Trả về HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseErrorDto> handleGenericException(Exception ex) {
        ResponseErrorDto errorDto = ResponseErrorDto.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message("Đã xảy ra lỗi nội bộ hệ thống: " + ex.getMessage())
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
