package com.tecngo.referrals.service;

import com.tecngo.referrals.entity.*;
import com.tecngo.referrals.repository.*;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.users.entity.User;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReferralServiceTest {
    private final ReferralCodeRepository codes = mock(ReferralCodeRepository.class);
    private final ReferralRegistrationRepository registrations = mock(ReferralRegistrationRepository.class);
    private final ReferralRewardRepository rewards = mock(ReferralRewardRepository.class);
    private final SystemParameterService parameters = mock(SystemParameterService.class);
    private final ReferralService service = new ReferralService(codes, registrations, rewards, parameters);

    @Test
    void doesNotConsumeRewardWhenCommissionIsZero() {
        when(parameters.referralProgramEnabled()).thenReturn(true);
        when(parameters.referralRewardOnlyIfCommissionGtZero()).thenReturn(true);

        assertThat(service.useAvailableReward(User.builder().id(UUID.randomUUID()).build(),
                ServiceRequest.builder().id(UUID.randomUUID()).build(), BigDecimal.ZERO)).isNull();
        verifyNoInteractions(rewards);
    }

    @Test
    void consumesAvailableRewardWhenCommissionIsPositive() {
        UUID technicianId = UUID.randomUUID();
        User technician = User.builder().id(technicianId).build();
        ServiceRequest request = ServiceRequest.builder().id(UUID.randomUUID()).build();
        ReferralReward reward = ReferralReward.builder().id(UUID.randomUUID())
                .status(ReferralRewardStatus.AVAILABLE).build();
        when(parameters.referralProgramEnabled()).thenReturn(true);
        when(parameters.referralRewardOnlyIfCommissionGtZero()).thenReturn(true);
        when(rewards.findAvailableForUpdate(technicianId)).thenReturn(List.of(reward));
        when(rewards.save(reward)).thenReturn(reward);

        assertThat(service.useAvailableReward(technician, request, BigDecimal.TEN)).isSameAs(reward);
        assertThat(reward.getStatus()).isEqualTo(ReferralRewardStatus.USED);
        assertThat(reward.getUsedServiceRequest()).isSameAs(request);
    }
}
