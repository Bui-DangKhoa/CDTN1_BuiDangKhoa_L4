package com.mekongmobile.smartcrm.exceptions;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Cấu trúc JSON chuẩn cho phản hồi lỗi API tầng Service / Rest API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseErrorDto {

    private String code;
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
