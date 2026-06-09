package com.tecngo.services.repository;

import com.tecngo.services.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, UUID> {
    boolean existsBySlug(String slug);
    boolean existsByNameIgnoreCase(String name);
    List<ServiceCategory> findByActiveTrueOrderByNameAsc();
}
