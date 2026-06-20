package com.tecngo.users.service;

import com.tecngo.users.entity.ActiveMode;
import com.tecngo.users.entity.User;
import com.tecngo.users.entity.UserActiveModeAudit;
import com.tecngo.users.repository.UserActiveModeAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActiveModeAuditService {
    private final UserActiveModeAuditRepository audits;

    public UserActiveModeAudit record(User user, User changedBy, ActiveMode previousMode,
                                      ActiveMode newMode, String reason) {
        return audits.save(UserActiveModeAudit.builder()
                .user(user)
                .changedBy(changedBy)
                .previousMode(previousMode)
                .newMode(newMode)
                .reason(reason)
                .build());
    }
}
