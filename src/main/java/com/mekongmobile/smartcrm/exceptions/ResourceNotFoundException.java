package com.mekongmobile.smartcrm.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Ngoại lệ ném ra khi tài nguyên không tìm thấy (404 Not Found).
 */
public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(String.format("%s với định danh '%s' không tồn tại trên hệ thống", resourceName, identifier),
              HttpStatus.NOT_FOUND, 
              "RESOURCE_NOT_FOUND");
    }
}
