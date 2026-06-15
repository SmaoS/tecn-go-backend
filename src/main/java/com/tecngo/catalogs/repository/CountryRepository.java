package com.tecngo.catalogs.repository;

import com.tecngo.catalogs.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CountryRepository extends JpaRepository<Country, UUID> {
    List<Country> findByActiveTrueOrderByNameAsc();
}
