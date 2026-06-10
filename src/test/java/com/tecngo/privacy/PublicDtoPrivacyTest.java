package com.tecngo.privacy;

import com.tecngo.service_requests.dto.ServiceRequestResponse;
import com.tecngo.users.dto.UserVerificationResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PublicDtoPrivacyTest {
    @Test
    void serviceRequestPublicDtoDoesNotExposePrivateEvidence() {
        var fields = Arrays.stream(ServiceRequestResponse.class.getRecordComponents())
                .map(component -> component.getName())
                .toList();

        assertThat(fields).doesNotContain("documentPhotoUrl", "certificatePhotoUrl");
    }

    @Test
    void administrativeVerificationDtoKeepsEvidenceForReviewers() {
        var fields = Arrays.stream(UserVerificationResponse.class.getRecordComponents())
                .map(component -> component.getName())
                .toList();

        assertThat(fields).contains("documentPhotoUrl", "certificatePhotoUrl");
    }
}
