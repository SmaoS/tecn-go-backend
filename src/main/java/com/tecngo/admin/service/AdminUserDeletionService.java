package com.tecngo.admin.service;

import com.tecngo.admin.dto.AdminUserDeletionResponse;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserDeletionService {
    private final UserRepository users;
    private final JdbcTemplate jdbc;

    @Transactional
    public AdminUserDeletionResponse deleteInitialRegistration(UUID userId, User admin) {
        if (!admin.hasRole(Role.ADMIN)) {
            throw new ForbiddenException("Admin role is required");
        }
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.hasRole(Role.ADMIN) || user.hasRole(Role.VERIFIER)) {
            throw new ConflictException("No se pueden eliminar cuentas administrativas desde esta acción");
        }
        assertNoOperationalData(user.getId());

        String email = user.getEmail();
        try {
            deleteOnboardingData(user.getId());
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("El usuario tiene relaciones adicionales. Usa anonimización o revisa el historial antes de eliminarlo");
        }
        return new AdminUserDeletionResponse(user.getId(), email, "Usuario eliminado correctamente");
    }

    private void assertNoOperationalData(UUID userId) {
        assertNoRows("service_requests", "client_id = ? OR technician_id = ?", userId, userId);
        assertNoRows("payments", "client_id = ? OR technician_id = ?", userId, userId);
        assertNoRows("ratings", "rater_id = ? OR rated_user_id = ?", userId, userId);
        assertNoRows("service_quotes", "technician_id = ?", userId);
        assertNoRows("chat_messages", "sender_id = ?", userId);
        assertNoRows("user_reports", "reporter_user_id = ? OR reported_user_id = ?", userId, userId);
        assertNoRows("compliance_data_requests", "user_id = ?", userId);
    }

    private void assertNoRows(String table, String whereClause, Object... args) {
        Long count = jdbc.queryForObject("select count(*) from " + table + " where " + whereClause, Long.class, args);
        if (count != null && count > 0) {
            throw new ConflictException("No se puede eliminar: el usuario ya tiene datos operativos en " + table);
        }
    }

    private void deleteOnboardingData(UUID userId) {
        jdbc.update("delete from notifications where user_id = ?", userId);
        jdbc.update("""
                delete from technician_profile_categories
                where technician_profile_id in (
                    select id from technician_profiles where user_id = ?
                )
                """, userId);
        jdbc.update("delete from technician_profiles where user_id = ?", userId);
        jdbc.update("delete from legal_acceptances where user_id = ?", userId);
        jdbc.update("delete from referral_codes where technician_id = ?", userId);
        jdbc.update("delete from content_reports where content_asset_id in (select id from content_assets where uploaded_by_user_id = ?)", userId);
        jdbc.update("delete from content_assets where uploaded_by_user_id = ?", userId);
        jdbc.update("delete from compliance_access_audits where actor_user_id = ? or subject_user_id = ?", userId, userId);
        jdbc.update("delete from profile_selfie_change_requests where user_id = ?", userId);
        jdbc.update("delete from users where id = ?", userId);
    }
}
