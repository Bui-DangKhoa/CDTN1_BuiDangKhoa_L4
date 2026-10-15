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
}
