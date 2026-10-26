package com.mekongmobile.smartcrm.exceptions;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Cấu trúc JSON chuẩn cho phản hồi lỗi API: { "error": "MA_LOI", "message": "Thông báo" }.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseErrorDto {

    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;

    @JsonProperty("requestId")
    private String requestId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // Khả năng tương thích ngược cho các đoạn mã cũ sử dụng code
    public String getCode() {
        return error;
    }

    public void setCode(String code) {
        this.error = code;
    }
}
