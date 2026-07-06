package com.tecngo.compliance.service;

import com.tecngo.compliance.dto.*;
import com.tecngo.compliance.repository.ComplianceRetentionPolicyRepository;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.sql.Timestamp;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RetentionService {
    private final ComplianceRetentionPolicyRepository policies;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<RetentionPolicyResponse> policies() {
        return policies.findAll().stream()
                .sorted(Comparator.comparing(item -> item.getDataCategory()))
                .map(this::map).toList();
    }

    @Transactional
    public RetentionPolicyResponse update(String category, RetentionPolicyRequest request, User admin) {
        var policy = policies.findByDataCategory(category.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Retention policy not found"));
        policy.setRetentionDays(request.retentionDays());
        policy.setLegalBasis(request.legalBasis().trim());
        policy.setAutomaticDeletion(request.automaticDeletion());
        policy.setActive(request.active());
        policy.setUpdatedBy(admin);
        return map(policies.save(policy));
    }

    @Transactional
    public Map<String, Integer> execute() {
        Map<String, Integer> result = new LinkedHashMap<>();
        policies.findByActiveTrueOrderByDataCategory().stream()
                .filter(item -> item.isAutomaticDeletion())
                .forEach(policy -> result.put(policy.getDataCategory(), purge(policy.getDataCategory(),
                        Instant.now().minus(policy.getRetentionDays(), ChronoUnit.DAYS))));
        return result;
    }

    @Scheduled(cron = "${app.compliance.retention-cron:0 35 3 * * *}")
    public void scheduledRetention() {
        execute();
    }

    private int purge(String category, Instant cutoff) {
        Timestamp cutoffTimestamp = Timestamp.from(cutoff);
        return switch (category) {
            case "ACCESS_AUDIT" ->
                    jdbc.update("delete from compliance_access_audits where created_at < ?", cutoffTimestamp);
            case "NOTIFICATIONS" ->
                    jdbc.update("delete from notifications where is_read = true and created_at < ?", cutoffTimestamp);
            case "AUTHENTICATION_METADATA" -> {
                int deleted = jdbc.update("delete from auth_sessions where expires_at < ?", cutoffTimestamp);
                deleted += jdbc.update("""
                        delete from verification_tokens
                         where expires_at < ? or used_at is not null
                        """, cutoffTimestamp);
                deleted += jdbc.update("""
                        delete from password_reset_tokens
                         where expires_at < ? or used = true
                        """, cutoffTimestamp);
                yield deleted;
            }
            default -> 0;
        };
    }

    private RetentionPolicyResponse map(com.tecngo.compliance.entity.ComplianceRetentionPolicy item) {
        return new RetentionPolicyResponse(item.getId(), item.getDataCategory(),
                item.getRetentionDays(), item.getLegalBasis(), item.isAutomaticDeletion(),
                item.isActive(), item.getUpdatedAt());
    }
}
