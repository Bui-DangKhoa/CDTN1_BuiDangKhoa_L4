package com.mekongmobile.smartcrm.repositories;

import com.mekongmobile.smartcrm.entities.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TẦNG 3: Repository quản lý thông tin thiết bị (Device).
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findBySerialNo(String serialNo);

    List<Device> findByCustomerId(Long customerId);
}
