package com.tecngo.technicians.service;

import com.tecngo.catalogs.service.GeographicCatalogService;
import com.tecngo.content_moderation.service.ManagedContentPolicy;
import com.tecngo.referrals.service.ReferralService;
import com.tecngo.services.service.ServiceCategoryService;
import com.tecngo.shared.exception.CodedForbiddenException;
import com.tecngo.technician_wallet.service.TechnicianWalletService;
import com.tecngo.technicians.entity.TechnicianProfile;
import com.tecngo.technicians.entity.TechnicianStatus;
import com.tecngo.technicians.repository.TechnicianProfileRepository;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import com.tecngo.users.service.UserService;
import com.tecngo.verification.service.EmailVerificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicianProfileOperationalGuardTest {
    @Mock TechnicianProfileRepository profiles;
    @Mock ServiceCategoryService categoryService;
    @Mock UserRepository users;
    @Mock UserService userService;
    @Mock EmailVerificationService emailVerification;
    @Mock ReferralService referrals;
    @Mock ManagedContentPolicy managedContent;
    @Mock GeographicCatalogService geographicCatalogs;
    @Mock TechnicianWalletService wallets;
    @InjectMocks TechnicianProfileService service;

    @Test
    void incompleteTechnicianCannotOperate() {
        User technician = User.builder()
                .id(UUID.randomUUID())
                .role(Role.TECHNICIAN)
                .emailVerified(true)
                .onboardingCompleted(false)
                .build();
        TechnicianProfile profile = TechnicianProfile.builder()
                .user(technician)
                .status(TechnicianStatus.APPROVED)
                .categories(new HashSet<>())
                .build();
        when(profiles.findByUserId(technician.getId())).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.approvedProfile(technician))
                .isInstanceOf(CodedForbiddenException.class)
                .hasMessage("Completa tu perfil técnico para poder operar.")
                .satisfies(error -> {
                    CodedForbiddenException coded = (CodedForbiddenException) error;
                    org.assertj.core.api.Assertions.assertThat(coded.getCode())
                            .isEqualTo("TECHNICIAN_PROFILE_INCOMPLETE");
                });
    }
}
