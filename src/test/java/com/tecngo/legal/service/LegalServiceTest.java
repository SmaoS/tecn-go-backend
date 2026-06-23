package com.tecngo.legal.service;

import com.tecngo.legal.entity.LegalDocument;
import com.tecngo.legal.entity.LegalRoleTarget;
import com.tecngo.legal.repository.LegalAcceptanceRepository;
import com.tecngo.legal.repository.LegalDocumentRepository;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalServiceTest {
    @Mock LegalDocumentRepository documents;
    @Mock LegalAcceptanceRepository acceptances;
    @Mock SystemParameterService parameters;
    @InjectMocks LegalService service;

    @Test
    void activeIncludesDocumentsForEveryEffectiveRole() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .role(Role.CLIENT)
                .roles(Set.of(Role.CLIENT, Role.TECHNICIAN))
                .build();
        LegalDocument clientTerms = document("CLIENT_TERMS", LegalRoleTarget.CLIENT);
        LegalDocument technicianTerms = document("TECHNICIAN_TERMS", LegalRoleTarget.TECHNICIAN);
        LegalDocument privacy = document("PRIVACY_POLICY", LegalRoleTarget.ALL);
        when(documents.findByActiveTrueOrderByCodeAsc())
                .thenReturn(List.of(clientTerms, technicianTerms, privacy));

        var result = service.active(user);

        assertThat(result).extracting(item -> item.code())
                .containsExactly("CLIENT_TERMS", "TECHNICIAN_TERMS", "PRIVACY_POLICY");
    }

    @Test
    void requireAcceptedReturnsStableCodeForClientsWithPendingDocuments() {
        User user = User.builder().id(UUID.randomUUID()).role(Role.CLIENT).roles(Set.of(Role.CLIENT)).build();
        LegalDocument privacy = document("PRIVACY_POLICY", LegalRoleTarget.ALL);
        when(parameters.requireLegalAcceptance()).thenReturn(true);
        when(documents.findByActiveTrueOrderByCodeAsc()).thenReturn(List.of(privacy));

        assertThatThrownBy(() -> service.requireAccepted(user))
                .isInstanceOf(com.tecngo.shared.exception.ConflictException.class)
                .satisfies(error -> assertThat(
                        ((com.tecngo.shared.exception.ConflictException) error).getCode())
                        .isEqualTo("LEGAL_ACCEPTANCE_REQUIRED"));
    }

    private LegalDocument document(String code, LegalRoleTarget roleTarget) {
        return LegalDocument.builder()
                .id(UUID.randomUUID())
                .code(code)
                .title(code)
                .version("1.0")
                .roleTarget(roleTarget)
                .content("Contenido")
                .active(true)
                .build();
    }
}
