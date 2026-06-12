package com.tecngo.app_versions.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

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
}
