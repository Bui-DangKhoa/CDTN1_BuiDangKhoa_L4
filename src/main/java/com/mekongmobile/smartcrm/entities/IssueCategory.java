package com.mekongmobile.smartcrm.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * Thực thể Danh mục sự cố kỹ thuật (issue_category).
 */
@Entity
@Table(name = "issue_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "category_name", nullable = false, length = 60, unique = true)
    private String categoryName;

    @Column(name = "default_priority", nullable = false, length = 10)
    private String defaultPriority;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
