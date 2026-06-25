package com.tecngo.admin.service;

import com.tecngo.admin.dto.AdminUserSearchResponse;
import com.tecngo.users.entity.*;
import com.tecngo.users.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserSearchService {
    private final UserRepository users;

    @Transactional(readOnly = true)
    public Page<AdminUserSearchResponse> search(Role role,
                                                AccountStatus status,
                                                Instant createdFrom,
                                                Instant createdTo,
                                                String search,
                                                Pageable pageable) {
        String searchPattern = search == null || search.isBlank()
                ? null
                : "%" + search.trim().toLowerCase() + "%";
        return users.findAll(specification(role, status, createdFrom, createdTo, searchPattern), pageable)
                .map(this::map);
    }

    private Specification<User> specification(Role role,
                                              AccountStatus status,
                                              Instant createdFrom,
                                              Instant createdTo,
                                              String searchPattern) {
        return (root, query, criteria) -> {
            if (query != null) query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) {
                var roleJoin = root.join("roles", JoinType.LEFT);
                predicates.add(criteria.or(
                        criteria.equal(root.get("role"), role),
                        criteria.equal(roleJoin, role)
                ));
            }
            if (status != null) {
                predicates.add(criteria.equal(root.get("accountStatus"), status));
            }
            if (createdFrom != null) {
                predicates.add(criteria.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(criteria.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }
            if (searchPattern != null) {
                predicates.add(criteria.or(
                        criteria.like(criteria.lower(root.get("fullName")), searchPattern),
                        criteria.like(criteria.lower(criteria.coalesce(root.get("email"), "")), searchPattern),
                        criteria.like(criteria.coalesce(root.get("phone"), ""), searchPattern)
                ));
            }
            return predicates.isEmpty()
                    ? criteria.conjunction()
                    : criteria.and(predicates.toArray(Predicate[]::new));
        };
    }

    private AdminUserSearchResponse map(User user) {
        List<String> comments = onboardingComments(user);
        return new AdminUserSearchResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getProfilePhotoUrl(),
                user.getRole(),
                user.getEffectiveRoles(),
                user.getAccountStatus(),
                user.getVerificationStatus(),
                user.isOnboardingCompleted(),
                user.getOnboardingStep(),
                user.isOnboardingCompleted() ? "COMPLETED" : "PENDING",
                comments,
                user.getCreatedAt()
        );
    }

    private List<String> onboardingComments(User user) {
        List<String> comments = new ArrayList<>();
        if (!user.isEmailVerified()) comments.add("Verificar correo electrónico");
        if (blank(user.getPhone())) comments.add("Registrar celular");
        if (!user.isPhoneVerified()) comments.add("Verificar celular por OTP");
        if (user.getCity() == null) comments.add("Seleccionar país, departamento y ciudad");
        if (blank(user.getHomeAddress())) comments.add("Registrar dirección");
        if (blank(user.getHomeNeighborhood())) comments.add("Registrar barrio");
        if (user.getDocumentType() == null || blank(user.getDocumentNumber())) {
            comments.add("Completar tipo y número de documento");
        }
        if (blank(user.getProfilePhotoUrl())) comments.add("Tomar o subir foto de perfil/selfie");
        if (user.getDocumentType() == DocumentType.CC
                && (blank(user.getDocumentFrontUrl()) || blank(user.getDocumentBackUrl()))) {
            comments.add("Subir documento por ambos lados");
        } else if (user.getDocumentType() == DocumentType.PASSPORT && blank(user.getDocumentSingleUrl())) {
            comments.add("Subir documento de identidad");
        } else if (user.getDocumentType() == null && blank(user.getDocumentPhotoUrl())) {
            comments.add("Subir documento de identidad");
        }
        if (user.hasRole(Role.TECHNICIAN)) {
            if (blank(user.getWorkExperienceDescription())) {
                comments.add("Completar experiencia laboral del técnico");
            }
            if (user.getOnboardingStep() == OnboardingStep.TECHNICIAN_CERTIFICATE
                    && blank(user.getCertificatePhotoUrl())) {
                comments.add("Certificado de estudio opcional pendiente");
            }
        }
        if (user.getVerificationStatus() == VerificationStatus.PENDING_VERIFICATION) {
            comments.add("Pendiente de revisión por admin/verificador");
        }
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            comments.add("Cuenta no activa: " + user.getAccountStatus());
        }
        if (comments.isEmpty()) {
            comments.add(user.isOnboardingCompleted()
                    ? "Registro completo"
                    : "Continuar desde el paso " + user.getOnboardingStep());
        }
        return comments;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
