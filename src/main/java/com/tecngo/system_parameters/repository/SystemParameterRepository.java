package com.tecngo.system_parameters.repository;

import com.tecngo.system_parameters.entity.SystemParameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SystemParameterRepository extends JpaRepository<SystemParameter, UUID> {
    Optional<SystemParameter> findByKeyAndActiveTrue(String key);
}
