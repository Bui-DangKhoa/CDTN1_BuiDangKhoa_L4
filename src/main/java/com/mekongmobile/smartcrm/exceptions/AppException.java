package com.mekongmobile.smartcrm.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Lớp Exception cơ sở cho toàn bộ ứng dụng Smart CRM.
 */
@Getter
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public AppException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = mapStatus(errorCode);
    }

    public AppException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.errorCode = "BAD_REQUEST";
    }

    public AppException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public AppException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    private static HttpStatus mapStatus(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.BAD_REQUEST;
        }
        return switch (errorCode.toUpperCase()) {
            case "NOT_FOUND", "RESOURCE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "SCHEDULE_CONFLICT", "CONFLICT" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
