package com.tecngo.content_moderation.service;

import com.tecngo.content_moderation.entity.ContentAsset;
import com.tecngo.content_moderation.entity.ContentAssetKind;
import com.tecngo.content_moderation.entity.ModerationStatus;
import com.tecngo.content_moderation.repository.ContentAssetRepository;
import com.tecngo.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManagedContentPolicyTest {
    private final ContentAssetRepository assets = mock(ContentAssetRepository.class);
    private final ManagedContentPolicy policy = new ManagedContentPolicy(assets);
    private final User owner = User.builder().id(UUID.randomUUID()).build();

    @BeforeEach
    void enableManagedUploads() {
        ReflectionTestUtils.setField(policy, "requireManagedUploads", true);
    }

    @Test
    void acceptsOwnedModeratedUploadForExpectedPurpose() {
        String url = "/v1/files/private-profile";
        when(assets.findByFileUrl(url)).thenReturn(Optional.of(ContentAsset.builder()
                .uploadedBy(owner)
                .kind(ContentAssetKind.PROFILE)
                .moderationStatus(ModerationStatus.APPROVED)
                .build()));

        assertThat(policy.validateChange(null, url, owner, Set.of(ContentAssetKind.PROFILE)))
                .isEqualTo(url);
    }

    @Test
    void blocksExternalUrlsAndRejectedUploads() {
        assertThatThrownBy(() -> policy.validateChange(null, "https://example.com/photo.jpg",
                owner, Set.of(ContentAssetKind.PROFILE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("upload endpoint");

        String url = "/v1/files/private-rejected";
        when(assets.findByFileUrl(url)).thenReturn(Optional.of(ContentAsset.builder()
                .uploadedBy(owner)
                .kind(ContentAssetKind.PROFILE)
                .moderationStatus(ModerationStatus.REJECTED)
                .build()));
        assertThatThrownBy(() -> policy.validateChange(null, url, owner,
                Set.of(ContentAssetKind.PROFILE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rejected");
    }
}
