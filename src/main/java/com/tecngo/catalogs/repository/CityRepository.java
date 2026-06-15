package com.tecngo.catalogs.repository;

import com.tecngo.catalogs.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CityRepository extends JpaRepository<City, UUID> {
    List<City> findByDepartmentIdAndActiveTrueOrderByNameAsc(UUID departmentId);
}
