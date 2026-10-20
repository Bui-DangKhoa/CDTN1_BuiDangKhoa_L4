package com.mekongmobile.smartcrm.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * Thực thể Tài khoản Người dùng nội bộ (Quản lý, Kỹ thuật viên) phục vụ Xác thực và Phân quyền.
 */
@Entity
@Table(name = "app_user", uniqueConstraints = {
        @UniqueConstraint(name = "uk_app_user_username", columnNames = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    /**
     * Vai trò hệ thống: QUAN_LY (Quản lý trung tâm), KY_THUAT_VIEN (Kỹ thuật viên)
     */
    @Column(name = "role", nullable = false, length = 30)
    private String role;

    @Column(name = "center_id", nullable = false)
    private Long centerId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
