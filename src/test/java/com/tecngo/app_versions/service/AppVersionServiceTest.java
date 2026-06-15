package com.tecngo.app_versions.service;

import com.tecngo.app_versions.entity.AppPlatform;
import com.tecngo.app_versions.entity.AppVersion;
import com.tecngo.app_versions.repository.AppVersionRepository;
import com.tecngo.system_parameters.service.SystemParameterService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppVersionServiceTest {
    private final AppVersionService service = new AppVersionService(null, null);

    @Test
    void comparesSemanticVersionsByNumericSegments() {
        assertThat(service.compare("1.0.10", "1.0.2")).isPositive();
        assertThat(service.compare("1.2.0", "1.10.0")).isNegative();
        assertThat(service.compare("2.0.0", "2.0.0")).isZero();
    }

    @Test
    void rejectsInvalidSemanticVersion() {
        assertThatThrownBy(() -> service.compare("1.0", "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inactiveConfigurationExplicitlyAllowsAppAccess() {
        AppVersionRepository repository = mock(AppVersionRepository.class);
        SystemParameterService parameters = mock(SystemParameterService.class);
        AppVersion item = AppVersion.builder()
                .platform(AppPlatform.ANDROID)
                .minimumSupportedVersion("2.0.0")
                .latestVersion("2.0.0")
                .forceUpdate(true)
                .updateUrl("https://play.google.com/store/apps/details?id=com.tecngo")
                .message("Actualiza")
                .active(false)
                .build();
        when(repository.findByPlatform(AppPlatform.ANDROID)).thenReturn(Optional.of(item));
        AppVersionService configuredService = new AppVersionService(repository, parameters);

        var response = configuredService.check(AppPlatform.ANDROID, "1.0.0");

        assertThat(response.updateRequired()).isFalse();
        assertThat(response.forceUpdate()).isFalse();
    }
}
