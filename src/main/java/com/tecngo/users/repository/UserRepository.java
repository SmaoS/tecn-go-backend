package com.tecngo.users.repository;

import com.tecngo.users.entity.User;
import com.tecngo.users.entity.AccountStatus;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByPhoneNormalized(String phoneNormalized);
    boolean existsByPhoneNormalized(String phoneNormalized);
    @Query("""
            select u from User u
            left join fetch u.country
            left join fetch u.department
            left join fetch u.city
            where u.id = :id
            """)
    Optional<User> findProfileById(@Param("id") UUID id);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByProfilePhotoUrl(String url);
    Optional<User> findByProfilePhotoUrl(String url);
    boolean existsByDocumentPhotoUrlOrCertificatePhotoUrl(String documentUrl, String certificateUrl);
    List<User> findByVerificationStatusOrderByCreatedAtAsc(VerificationStatus status);
    long countByVerificationStatus(VerificationStatus status);
    List<User> findByRoleOrderByCreatedAtDesc(Role role);
    List<User> findByRoleInOrderByCreatedAtDesc(Set<Role> roles);
    List<User> findByAccountStatusNotOrderByCreatedAtDesc(AccountStatus status);
}
