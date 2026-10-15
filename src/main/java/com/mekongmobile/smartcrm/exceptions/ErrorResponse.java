package com.mekongmobile.smartcrm.exceptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cấu trúc chuẩn cho phản hồi lỗi API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private boolean success;
    private String errorCode;
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private Map<String, String> errors;
}
