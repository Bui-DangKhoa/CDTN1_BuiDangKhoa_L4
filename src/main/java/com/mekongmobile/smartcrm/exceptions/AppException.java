package com.mekongmobile.smartcrm.exceptions;

import lombok.Getter;

/**
 * Lớp Exception cơ sở cho toàn bộ ứng dụng Smart CRM.
 * Tuân thủ quy tắc tách lớp (C2, S2): Tầng Service chỉ ném errorCode nghiệp vụ,
 * không phụ thuộc vào HttpStatus của tầng giao vận HTTP.
 */
@Getter
public class AppException extends RuntimeException {

    private final String errorCode;

    public AppException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AppException(String message) {
        super(message);
        this.errorCode = "BAD_REQUEST";
    }
}
