package com.mekongmobile.smartcrm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * TẦNG 1: DTO lưu trữ thông tin phiên làm việc của người dùng đăng nhập trong HttpSession.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionDto implements Serializable {

    private Long userId;
    private String username;
    private String fullName;
    private String role; // "QUAN_LY" hoặc "KY_THUAT_VIEN"
    private Long centerId;
}
