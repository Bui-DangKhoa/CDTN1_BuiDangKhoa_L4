package com.mekongmobile.smartcrm.repositories;

import com.mekongmobile.smartcrm.entities.IssueCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TẦNG 3: Repository quản lý danh mục sự cố (IssueCategory).
 */
@Repository
public interface IssueCategoryRepository extends JpaRepository<IssueCategory, Integer> {

    Optional<IssueCategory> findByCategoryName(String categoryName);

    List<IssueCategory> findByIsActiveTrue();
}
