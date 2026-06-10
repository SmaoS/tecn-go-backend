package com.tecngo.admin.service;

import com.tecngo.admin.dto.CreateVerifierRequest;
import com.tecngo.admin.dto.VerifierResponse;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.entity.VerificationStatus;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VerifierService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public VerifierResponse create(CreateVerifierRequest request, User admin) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email is already registered");
        }
        User verifier = users.save(User.builder()
                .fullName(request.fullName().trim())
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.VERIFIER)
                .verificationStatus(VerificationStatus.VERIFIED)
                .verifiedAt(java.time.Instant.now())
                .verifiedBy(admin)
                .build());
        return map(verifier);
    }

    @Transactional(readOnly = true)
    public List<VerifierResponse> list() {
        return users.findByRoleOrderByCreatedAtDesc(Role.VERIFIER).stream().map(this::map).toList();
    }

    private VerifierResponse map(User user) {
        return new VerifierResponse(user.getId(), user.getFullName(), user.getEmail(), user.getCreatedAt());
    }
}
