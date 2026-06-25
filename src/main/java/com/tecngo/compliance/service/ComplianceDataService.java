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
import com.tecngo.verification.service.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    private final EmailSender emailSender;

    @Value("${app.public-api-url:${APP_PUBLIC_API_URL:}}")
    private String publicApiUrl;

    @Transactional
    public DataRequestResponse requestExport(User user, String correlationId) {
        if (requests.existsByUserIdAndRequestTypeAndStatus(
                user.getId(), DataRequestType.EXPORT, DataRequestStatus.PENDING)) {
            throw new ConflictException("A data export request is already pending");
        }
        ComplianceDataRequest request = requests.save(ComplianceDataRequest.builder()
                .user(user).requestType(DataRequestType.EXPORT)
                .status(DataRequestStatus.PENDING).build());
        audits.record(user, user, "PERSONAL_DATA", user.getId().toString(),
                "EXPORT_REQUEST", AuditOutcome.SUCCESS, correlationId, null,
                "User requested personal data export");
        return map(request);
    }

    @Transactional(readOnly = true)
    public List<DataRequestResponse> exportRequests(User user) {
        return requests.findByUserIdOrderByRequestedAtDesc(user.getId()).stream()
                .filter(item -> item.getRequestType() == DataRequestType.EXPORT)
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DataRequestResponse> exportRequests(DataRequestStatus status) {
        DataRequestStatus effectiveStatus = status == null ? DataRequestStatus.PENDING : status;
        return requests.findByRequestTypeAndStatusOrderByRequestedAtAsc(
                        DataRequestType.EXPORT, effectiveStatus).stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public DataRequestResponse approveExport(UUID requestId, User reviewer, String correlationId) {
        ComplianceDataRequest request = requireRequest(requestId);
        if (request.getRequestType() != DataRequestType.EXPORT
                || request.getStatus() != DataRequestStatus.PENDING) {
            throw new ConflictException("Only pending data export requests can be approved");
        }
        User target = request.getUser();
        if (target.getEmail() == null || target.getEmail().isBlank()) {
            throw new ConflictException("The user does not have a registered email");
        }
        request.setStatus(DataRequestStatus.APPROVED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(Instant.now());

        FileStorage.StoredFile stored = storage.store(new BytesMultipartFile(
                "file",
                "tecngo-data-export-" + target.getId() + ".zip",
                "application/zip",
                buildExportZip(target)
        ), false, "tecngo/data-exports", Set.of("application/zip", "text/plain", "text/csv"));
        request.setExportFileUrl(stored.accessUrl());
        request.setStatus(DataRequestStatus.SENT);
        request.setSentAt(Instant.now());
        request.setCompletedAt(request.getSentAt());
        emailSender.sendDataExport(target.getEmail(), target.getFullName(), absoluteUrl(stored.accessUrl()));
        audits.record(reviewer, target, "PERSONAL_DATA", target.getId().toString(),
                "EXPORT_APPROVE_SEND", AuditOutcome.SUCCESS, correlationId, null,
                "Personal data export generated and email sent");
        return map(requests.save(request));
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
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        request.setStatus(DataRequestStatus.REJECTED);
        request.setRejectionReason(clean(reason));
        request.setCompletedAt(Instant.now());
        request.setReviewedAt(request.getCompletedAt());
        request.setReviewedBy(reviewer);
        return map(requests.save(request));
    }

    private byte[] buildExportZip(User user) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            writeText(zip, "resumen.txt", """
                    Exportación de datos TecnGo
                    Usuario: %s
                    Fecha de generación: %s

                    Este archivo contiene datos personales y operativos asociados a la cuenta.
                    Los documentos sensibles no se incluyen como archivos adjuntos; se listan referencias, tipo y estado.
                    """.formatted(user.getFullName(), Instant.now()));
            writeCsv(zip, "datos-personales.csv", rows("""
                    select id, full_name, email, phone, phone_verified, email_verified,
                           verification_status, account_status, average_rating,
                           completed_services_count, paid_services_count, created_at,
                           home_address, home_city, home_neighborhood
                      from users where id = ?
                    """, user.getId()));
            writeCsv(zip, "roles.csv", rows("select role from user_roles where user_id = ? order by role", user.getId()));
            writeCsv(zip, "solicitudes.csv", rows("""
                    select id, category_id, technician_id, status, description, address,
                           estimated_price, technician_price, final_price, created_at
                      from service_requests
                     where client_id = ? or technician_id = ?
                     order by created_at desc
                    """, user.getId(), user.getId()));
            writeCsv(zip, "cotizaciones.csv", rows("""
                    select id, service_request_id, price, description, status, created_at, responded_at
                      from service_quotes where technician_id = ? order by created_at desc
                    """, user.getId()));
            writeCsv(zip, "pagos.csv", rows("""
                    select id, service_request_id, amount, platform_fee, technician_amount,
                           status, method, created_at
                      from payments where client_id = ? or technician_id = ?
                     order by created_at desc
                    """, user.getId(), user.getId()));
            writeCsv(zip, "calificaciones.csv", rows("""
                    select id, service_request_id, rater_id, rated_user_id, score, comment, created_at
                      from ratings where rater_id = ? or rated_user_id = ? order by created_at desc
                    """, user.getId(), user.getId()));
            writeCsv(zip, "referidos.csv", rows("""
                    select rr.id, rr.reward_type, rr.status, rr.created_at, rr.used_at, rr.expires_at
                      from referral_rewards rr
                     where rr.technician_id = ?
                     order by rr.created_at desc
                    """, user.getId()));
            writeCsv(zip, "denuncias.csv", rows("""
                    select id, service_request_id, reporter_user_id, reported_user_id,
                           reason, description, status, severity, created_at
                      from user_reports
                     where reporter_user_id = ? or reported_user_id = ?
                     order by created_at desc
                    """, user.getId(), user.getId()));
            writeCsv(zip, "aceptaciones-legales.csv", rows("""
                    select la.id, ld.code, ld.title, ld.version, la.accepted_at
                      from legal_acceptances la
                      join legal_documents ld on ld.id = la.legal_document_id
                     where la.user_id = ? order by la.accepted_at desc
                    """, user.getId()));
            writeCsv(zip, "archivos-y-evidencias.csv", rows("""
                    select id, kind,
                           case when kind in ('PROFILE','DOCUMENT','CERTIFICATE') then null else file_url end as file_url,
                           content_type, moderation_status, created_at
                      from content_assets where uploaded_by_user_id = ? order by created_at desc
                    """, user.getId()));
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to build data export", exception);
        }
    }

    private void writeText(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void writeCsv(ZipOutputStream zip, String name, List<Map<String, Object>> rows) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        if (!rows.isEmpty()) {
            List<String> headers = new ArrayList<>(rows.getFirst().keySet());
            zip.write((String.join(",", headers) + "\n").getBytes(StandardCharsets.UTF_8));
            for (Map<String, Object> row : rows) {
                List<String> values = headers.stream()
                        .map(header -> csv(row.get(header)))
                        .toList();
                zip.write((String.join(",", values) + "\n").getBytes(StandardCharsets.UTF_8));
            }
        }
        zip.closeEntry();
    }

    private String csv(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
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
                item.getReviewedAt(), item.getRejectionReason(), item.getExportFileUrl(), item.getSentAt(),
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

    private String absoluteUrl(String accessUrl) {
        if (accessUrl == null || accessUrl.startsWith("http")) return accessUrl;
        if (publicApiUrl == null || publicApiUrl.isBlank()) return accessUrl;
        return publicApiUrl.replaceAll("/+$", "") + accessUrl;
    }

    private record BytesMultipartFile(String name, String originalFilename, String contentType, byte[] bytes)
            implements MultipartFile {
        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return bytes.length == 0; }
        @Override public long getSize() { return bytes.length; }
        @Override public byte[] getBytes() { return bytes; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        @Override public void transferTo(File dest) throws IOException { Files.write(dest.toPath(), bytes); }
        @Override public void transferTo(Path dest) throws IOException { Files.write(dest, bytes); }
    }
}
