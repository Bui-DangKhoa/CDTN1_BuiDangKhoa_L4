package com.mekongmobile.smartcrm.repositories;

import com.mekongmobile.smartcrm.entities.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * TẦNG 3: Repository quản lý dữ liệu tài khoản người dùng hệ thống (AppUser).
 */
@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Tìm tài khoản theo username đang hoạt động (isActive = true).
     */
    Optional<AppUser> findByUsernameAndIsActiveTrue(String username);

    /**
     * Tìm tài khoản theo username bất kể trạng thái.
     */
    Optional<AppUser> findByUsername(String username);
}
