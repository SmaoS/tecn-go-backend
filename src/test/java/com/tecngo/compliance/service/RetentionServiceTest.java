package com.tecngo.compliance.service;

import com.tecngo.compliance.entity.ComplianceRetentionPolicy;
import com.tecngo.compliance.repository.ComplianceRetentionPolicyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RetentionServiceTest {
    private final ComplianceRetentionPolicyRepository policies = mock(ComplianceRetentionPolicyRepository.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final RetentionService service = new RetentionService(policies, jdbc);

    @Test
    void onlyExecutesAllowlistedAutomaticRetentionPolicies() {
        when(policies.findByActiveTrueOrderByDataCategory()).thenReturn(List.of(
                policy("ACCESS_AUDIT", 730, true),
                policy("FINANCIAL_RECORDS", 3650, false),
                policy("UNKNOWN_CATEGORY", 10, true)
        ));
        when(jdbc.update(startsWith("delete from compliance_access_audits"), any(Object[].class)))
                .thenReturn(4);

        var result = service.execute();

        assertThat(result).containsEntry("ACCESS_AUDIT", 4)
                .containsEntry("UNKNOWN_CATEGORY", 0)
                .doesNotContainKey("FINANCIAL_RECORDS");
        verify(jdbc, never()).update(contains("payments"), any(Object[].class));
    }

    private ComplianceRetentionPolicy policy(String category, int days, boolean automatic) {
        return ComplianceRetentionPolicy.builder().dataCategory(category)
                .retentionDays(days).automaticDeletion(automatic).active(true).build();
    }
}
