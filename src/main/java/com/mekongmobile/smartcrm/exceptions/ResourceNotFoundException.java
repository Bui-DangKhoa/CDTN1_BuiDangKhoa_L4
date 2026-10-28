package com.mekongmobile.smartcrm.exceptions;

/**
 * Ngoại lệ ném ra khi tài nguyên không tìm thấy.
 * Tuân thủ quy tắc tách lớp (C2, S2): Ánh xạ sang HTTP 404 được thực hiện tại GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super("NOT_FOUND", String.format("%s với định danh '%s' không tồn tại trên hệ thống", resourceName, identifier));
    }

    public ResourceNotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
