package com.mekongmobile.smartcrm.services;

import com.mekongmobile.smartcrm.dto.UserSessionDto;
import com.mekongmobile.smartcrm.entities.Technician;
import com.mekongmobile.smartcrm.repositories.TechnicianRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * TẦNG 2: Hiện thực Service quản lý thông tin Kỹ thuật viên (Technician).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TechnicianServiceImpl implements TechnicianService {

    private final TechnicianRepository technicianRepository;

    @Override
    public Optional<Technician> findById(Long id) {
        return technicianRepository.findById(id);
    }

    @Override
    public Optional<Technician> findByUserId(Long userId) {
        return technicianRepository.findByUserId(userId);
    }

    @Override
    public Optional<Technician> findByFullName(String fullName) {
        return technicianRepository.findByFullName(fullName);
    }

    @Override
    public List<Technician> findActiveTechniciansByCenter(Long centerId) {
        return technicianRepository.findByCenterIdAndIsActiveTrue(centerId);
    }

    @Override
    public Optional<Technician> resolveTechnicianForUser(UserSessionDto currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            log.warn("resolveTechnicianForUser: currentUser hoặc userId bị null");
            return Optional.empty();
        }
        return technicianRepository.findByUserId(currentUser.getUserId());
    }
}
