package com.tecngo.users.repository;

import com.tecngo.users.entity.User;
import com.tecngo.users.entity.AccountStatus;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    @Query(value = """
            select distinct u from User u
            left join u.roles userRole
            where (:role is null or u.role = :role or userRole = :role)
              and (:status is null or u.accountStatus = :status)
              and (:createdFrom is null or u.createdAt >= :createdFrom)
              and (:createdTo is null or u.createdAt <= :createdTo)
              and (
                    :search is null
                    or lower(u.fullName) like lower(concat('%', :search, '%'))
                    or lower(coalesce(u.email, '')) like lower(concat('%', :search, '%'))
                    or coalesce(u.phone, '') like concat('%', :search, '%')
                  )
            """, countQuery = """
            select count(distinct u) from User u
            left join u.roles userRole
            where (:role is null or u.role = :role or userRole = :role)
              and (:status is null or u.accountStatus = :status)
              and (:createdFrom is null or u.createdAt >= :createdFrom)
              and (:createdTo is null or u.createdAt <= :createdTo)
              and (
                    :search is null
                    or lower(u.fullName) like lower(concat('%', :search, '%'))
                    or lower(coalesce(u.email, '')) like lower(concat('%', :search, '%'))
                    or coalesce(u.phone, '') like concat('%', :search, '%')
                  )
            """)
    Page<User> searchAdminUsers(@Param("role") Role role,
                                @Param("status") AccountStatus status,
                                @Param("createdFrom") Instant createdFrom,
                                @Param("createdTo") Instant createdTo,
                                @Param("search") String search,
                                Pageable pageable);
}
