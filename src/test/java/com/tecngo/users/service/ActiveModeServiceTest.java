package com.tecngo.users.service;

import com.tecngo.auth.service.JwtService;
import com.tecngo.users.entity.ActiveMode;
import com.tecngo.users.entity.OnboardingStep;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ActiveModeServiceTest {
    private UserRepository users;
    private ActiveModeAuditService audits;
    private JwtService jwtService;
    private ActiveModeService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        audits = mock(ActiveModeAuditService.class);
        jwtService = mock(JwtService.class);
        service = new ActiveModeService(users, audits, jwtService);
    }

    @Test
    void changesModeAuditsAndReturnsRefreshedToken() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .email("dual@tecngo.local")
                .role(Role.CLIENT)
                .roles(new LinkedHashSet<>(List.of(Role.CLIENT, Role.TECHNICIAN)))
                .activeMode(ActiveMode.CLIENT)
                .build();
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("refreshed-token");

        var response = service.change(user, ActiveMode.TECHNICIAN);

        assertThat(response.token()).isEqualTo("refreshed-token");
        assertThat(response.activeMode()).isEqualTo(ActiveMode.TECHNICIAN);
        assertThat(response.roles()).containsExactlyInAnyOrder(Role.CLIENT, Role.TECHNICIAN);
        assertThat(response.roleCreated()).isFalse();
        verify(users).save(user);
        verify(audits).record(user, user, ActiveMode.CLIENT, ActiveMode.TECHNICIAN,
                "USER_MODE_CHANGE");
    }

    @Test
    void createsMissingTechnicianCapabilityAndRequiresOnboarding() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .role(Role.CLIENT)
                .activeMode(ActiveMode.CLIENT)
                .onboardingCompleted(true)
                .onboardingStep(OnboardingStep.COMPLETED)
                .build();
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("technician-token");

        var response = service.change(user, ActiveMode.TECHNICIAN);

        assertThat(response.roleCreated()).isTrue();
        assertThat(response.roles()).containsExactlyInAnyOrder(Role.CLIENT, Role.TECHNICIAN);
        assertThat(response.onboardingCompleted()).isFalse();
        assertThat(response.onboardingStep()).isEqualTo(OnboardingStep.TECHNICIAN_PROFESSIONAL_PROFILE);
        assertThat(user.getActiveMode()).isEqualTo(ActiveMode.TECHNICIAN);
        verify(users).save(user);
        verify(audits).record(user, user, ActiveMode.CLIENT, ActiveMode.TECHNICIAN,
                "USER_CAPABILITY_CREATED_AND_MODE_CHANGED");
    }

    @Test
    void createsMissingClientCapabilityWithoutResettingCompletedOnboarding() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .role(Role.TECHNICIAN)
                .activeMode(ActiveMode.TECHNICIAN)
                .onboardingCompleted(true)
                .onboardingStep(OnboardingStep.COMPLETED)
                .build();
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("client-token");

        var response = service.change(user, ActiveMode.CLIENT);

        assertThat(response.roleCreated()).isTrue();
        assertThat(response.onboardingCompleted()).isTrue();
        assertThat(response.onboardingStep()).isEqualTo(OnboardingStep.COMPLETED);
        assertThat(response.activeMode()).isEqualTo(ActiveMode.CLIENT);
    }

    @Test
    void sameModeIsIdempotentAndDoesNotDuplicateAudit() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .role(Role.CLIENT)
                .activeMode(ActiveMode.CLIENT)
                .build();
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("same-mode-token");

        var response = service.change(user, ActiveMode.CLIENT);

        assertThat(response.activeMode()).isEqualTo(ActiveMode.CLIENT);
        assertThat(response.roleCreated()).isFalse();
        verify(users, never()).save(any());
        verifyNoInteractions(audits);
    }
}
