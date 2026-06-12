package com.tecngo.app_versions.repository;
import com.tecngo.app_versions.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AppVersionRepository extends JpaRepository<AppVersion, UUID> {
    Optional<AppVersion> findByPlatformAndActiveTrue(AppPlatform platform);
    Optional<AppVersion> findByPlatform(AppPlatform platform);
}
