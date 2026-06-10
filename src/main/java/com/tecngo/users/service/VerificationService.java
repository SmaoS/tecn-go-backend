package com.tecngo.users.service;

import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.users.dto.UserVerificationResponse;
import com.tecngo.users.entity.User;
import com.tecngo.users.entity.VerificationStatus;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationService {
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<UserVerificationResponse> pending() {
        return users.findByVerificationStatusOrderByCreatedAtAsc(VerificationStatus.PENDING_VERIFICATION)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public UserVerificationResponse verify(UUID userId, User reviewer) {
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getVerificationStatus() != VerificationStatus.PENDING_VERIFICATION) {
            throw new ConflictException("User is not pending verification");
        }
        if (user.getDocumentPhotoUrl() == null || user.getDocumentPhotoUrl().isBlank()) {
            throw new IllegalArgumentException("Document photo is required");
        }
        user.setVerificationStatus(VerificationStatus.VERIFIED);
        user.setVerifiedAt(Instant.now());
        user.setVerifiedBy(reviewer);
        return map(users.save(user));
    }

    private UserVerificationResponse map(User user) {
        return new UserVerificationResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getVerificationStatus(),
                user.getProfilePhotoUrl(),
                user.getDocumentPhotoUrl(),
                user.getCertificatePhotoUrl(),
                user.getWorkExperienceDescription(),
                user.getCreatedAt(),
                user.getVerifiedAt()
        );
    }
}
