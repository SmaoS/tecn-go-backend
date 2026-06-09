package com.tecngo.users.repository;

import com.tecngo.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByProfilePhotoUrl(String url);
    boolean existsByDocumentPhotoUrlOrCertificatePhotoUrl(String documentUrl, String certificateUrl);
}
