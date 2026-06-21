package com.tecngo.compliance.service;

import com.tecngo.auth.session.AuthSessionRepository;
import com.tecngo.compliance.dto.*;
import com.tecngo.compliance.entity.*;
import com.tecngo.compliance.repository.ComplianceDataRequestRepository;
import com.tecngo.content_moderation.entity.ContentAssetKind;
import com.tecngo.content_moderation.repository.ContentAssetRepository;
import com.tecngo.files.service.FileStorage;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.users.entity.*;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ComplianceDataService {
    private final ComplianceDataRequestRepository requests;
    private final UserRepository users;
    private final ContentAssetRepository assets;
    private final AuthSessionRepository sessions;
    private final FileStorage storage;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final ComplianceAuditService audits;

    @Transactional
    public DataExportResponse export(User user, String correlationId) {
        ComplianceDataRequest request = requests.save(ComplianceDataRequest.builder()
                .user(user).requestType(DataRequestType.EXPORT)
                .status(DataRequestStatus.COMPLETED).completedAt(Instant.now()).build());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("profile", one("""
                select id, full_name, email, phone, phone_verified, email_verified,
                       verification_status, account_status, average_rating,
                       completed_services_count, paid_services_count, created_at,
                       home_address, home_city, home_neighborhood
                  from users where id = ?
                """, user.getId()));
        data.put("roles", rows("select role from user_roles where user_id = ? order by role", user.getId()));
        data.put("serviceRequests", rows("""
                select id, category_id, technician_id, status, description, address,
                       estimated_price, technician_price, final_price, created_at
                  from service_requests
                 where client_id = ? or technician_id = ?
                 order by created_at desc
                """, user.getId(), user.getId()));
        data.put("quotes", rows("""
                select id, service_request_id, price, description, status, created_at, responded_at
                  from service_quotes where technician_id = ? order by created_at desc
                """, user.getId()));
        data.put("payments", rows("""
                select id, service_request_id, amount, platform_fee, technician_amount,
                       status, method, created_at
                  from payments where client_id = ? or technician_id = ?
                 order by created_at desc
                """, user.getId(), user.getId()));
        data.put("ratings", rows("""
                select id, service_request_id, rater_id, rated_user_id, score, comment, created_at
                  from ratings where rater_id = ? or rated_user_id = ? order by created_at desc
                """, user.getId(), user.getId()));
        data.put("notifications", rows("""
                select id, title, message, type, is_read, created_at
                  from notifications where user_id = ? order by created_at desc
                """, user.getId()));
        data.put("legalAcceptances", rows("""
                select la.id, ld.code, ld.title, ld.version, la.accepted_at
                  from legal_acceptances la
                  join legal_documents ld on ld.id = la.legal_document_id
                 where la.user_id = ? order by la.accepted_at desc
                """, user.getId()));
        data.put("uploadedAssets", rows("""
                select id, kind, file_url, content_type, moderation_status, created_at
                  from content_assets where uploaded_by_user_id = ? order by created_at desc
                """, user.getId()));
        audits.record(user, user, "PERSONAL_DATA", user.getId().toString(),
                "EXPORT", AuditOutcome.SUCCESS, correlationId, null,
                "User-generated personal data export");
        return new DataExportResponse(request.getId(), Instant.now(), data);
    }

    @Transactional
    public DataRequestResponse requestAnonymization(User user, String reason) {
        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.VERIFIER)) {
            throw new ConflictException("Staff accounts cannot be anonymized through self-service");
        }
        if (requests.existsByUserIdAndRequestTypeAndStatus(
                user.getId(), DataRequestType.ANONYMIZATION, DataRequestStatus.PENDING)) {
            throw new ConflictException("An anonymization request is already pending");
        }
        return map(requests.save(ComplianceDataRequest.builder()
                .user(user).requestType(DataRequestType.ANONYMIZATION)
                .status(DataRequestStatus.PENDING).reason(clean(reason)).build()));
    }

    @Transactional(readOnly = true)
    public List<DataRequestResponse> requests(DataRequestStatus status) {
        List<ComplianceDataRequest> items = status == null
                ? requests.findAll()
                : requests.findByStatusOrderByRequestedAtAsc(status);
        return items.stream().map(this::map).toList();
    }

    @Transactional
    public DataRequestResponse approveAnonymization(UUID requestId, User reviewer,
                                                     String correlationId) {
        ComplianceDataRequest request = requireRequest(requestId);
        if (request.getRequestType() != DataRequestType.ANONYMIZATION
                || request.getStatus() != DataRequestStatus.PENDING) {
            throw new ConflictException("Only pending anonymization requests can be approved");
        }
        User target = request.getUser();
        if (target.hasRole(Role.ADMIN) || target.hasRole(Role.VERIFIER)) {
            throw new ConflictException("Staff accounts require a separate offboarding process");
        }
        Integer active = jdbc.queryForObject("""
                select count(*) from service_requests
                 where (client_id = ? or technician_id = ?)
                   and status not in ('COMPLETED','PAID','CANCELLED','PAYMENT_DISPUTE')
                """, Integer.class, target.getId(), target.getId());
        if (active != null && active > 0) {
            throw new ConflictException("The user has active services and cannot be anonymized");
        }

        var personalAssets = assets.findByUploadedByIdAndKindIn(target.getId(),
                Set.of(ContentAssetKind.PROFILE, ContentAssetKind.DOCUMENT, ContentAssetKind.CERTIFICATE));
        for (var asset : personalAssets) {
            storage.delete(asset.getPublicId(), asset.getContentType());
            jdbc.update("delete from content_reports where content_asset_id = ?", asset.getId());
        }
        assets.deleteAll(personalAssets);

        String previousPhone = target.getPhoneNormalized();
        target.setFullName("Usuario anonimizado " + target.getId().toString().substring(0, 8));
        target.setEmail(null);
        target.setPhone(null);
        target.setPhoneNormalized(null);
        target.setDocumentNumber(null);
        target.setProfilePhotoUrl(null);
        target.setProfilePhotoPublicId(null);
        target.setDocumentPhotoUrl(null);
        target.setDocumentFrontUrl(null);
        target.setDocumentBackUrl(null);
        target.setDocumentSingleUrl(null);
        target.setCertificatePhotoUrl(null);
        target.setWorkExperienceDescription(null);
        target.setHomeAddress(null);
        target.setHomeCity(null);
        target.setHomeNeighborhood(null);
        target.setHomeLatitude(null);
        target.setHomeLongitude(null);
        target.setCountry(null);
        target.setDepartment(null);
        target.setCity(null);
        target.setFcmToken(null);
        target.setFcmTokenUpdatedAt(null);
        target.setEmailVerified(false);
        target.setPhoneVerified(false);
        target.setDocumentsVerified(false);
        target.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        target.setAccountStatus(AccountStatus.DELETED_LOGICAL);
        target.setInactiveComment("Personal data anonymized after approved request");
        target.setInactivatedAt(Instant.now());
        target.setInactivatedBy(reviewer);
        users.save(target);

        sessions.revokeAll(target.getId(), Instant.now(), "DATA_ANONYMIZATION");
        jdbc.update("delete from verification_tokens where user_id = ?", target.getId());
        jdbc.update("delete from password_reset_tokens where user_id = ?", target.getId());
        jdbc.update("delete from admin_mfa_challenges where user_id = ?", target.getId());
        jdbc.update("delete from notifications where user_id = ?", target.getId());
        if (previousPhone != null) {
            jdbc.update("delete from phone_otp_verifications where phone = ?", previousPhone);
        }

        request.setStatus(DataRequestStatus.COMPLETED);
        request.setCompletedAt(Instant.now());
        request.setReviewedBy(reviewer);
        audits.record(reviewer, target, "PERSONAL_DATA", target.getId().toString(),
                "ANONYMIZE", AuditOutcome.SUCCESS, correlationId, null,
                "Identity fields removed; financial and service records retained");
        return map(requests.save(request));
    }

    @Transactional
    public DataRequestResponse reject(UUID requestId, String reason, User reviewer) {
        ComplianceDataRequest request = requireRequest(requestId);
        if (request.getStatus() != DataRequestStatus.PENDING) {
            throw new ConflictException("Only pending requests can be rejected");
        }
        request.setStatus(DataRequestStatus.REJECTED);
        request.setReason(clean(reason));
        request.setCompletedAt(Instant.now());
        request.setReviewedBy(reviewer);
        return map(requests.save(request));
    }

    private ComplianceDataRequest requireRequest(UUID id) {
        return requests.findById(id).orElseThrow(() -> new NotFoundException("Data request not found"));
    }

    private List<Map<String, Object>> rows(String sql, Object... args) {
        return jdbc.queryForList(sql, args).stream().map(this::normalize).toList();
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> result = rows(sql, args);
        return result.isEmpty() ? Map.of() : result.getFirst();
    }

    private DataRequestResponse map(ComplianceDataRequest item) {
        return new DataRequestResponse(item.getId(), item.getUser().getId(),
                item.getUser().getFullName(), item.getRequestType(), item.getStatus(),
                item.getReason(), item.getRequestedAt(), item.getCompletedAt(),
                item.getReviewedBy() == null ? null : item.getReviewedBy().getId());
    }

    private Map<String, Object> normalize(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(camelCase(key.toLowerCase(Locale.ROOT)), value));
        return result;
    }

    private String camelCase(String value) {
        StringBuilder result = new StringBuilder();
        boolean upper = false;
        for (char character : value.toCharArray()) {
            if (character == '_') {
                upper = true;
            } else if (upper) {
                result.append(Character.toUpperCase(character));
                upper = false;
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        return trimmed.length() <= 1000 ? trimmed : trimmed.substring(0, 1000);
    }
}
