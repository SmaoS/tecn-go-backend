package com.tecngo.legal.service;
import com.tecngo.legal.dto.*;
import com.tecngo.legal.entity.*;
import com.tecngo.legal.repository.*;
import com.tecngo.shared.exception.*;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.users.entity.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class LegalService {
    private final LegalDocumentRepository documents;
    private final LegalAcceptanceRepository acceptances;
    private final SystemParameterService parameters;

    @Transactional(readOnly = true)
    public List<LegalDocumentResponse> active(User user) {
        if (user == null) {
            return publicActive();
        }
        return applicable(user).stream().map(item -> map(item,
                acceptances.existsByUserIdAndLegalDocumentId(user.getId(), item.getId()))).toList();
    }
    @Transactional(readOnly = true)
    public List<LegalDocumentResponse> publicActive() {
        return documents.findByActiveTrueOrderByCodeAsc().stream()
                .map(item -> map(item, false))
                .toList();
    }
    @Transactional
    public LegalDocumentResponse accept(UUID id, User user, HttpServletRequest request) {
        LegalDocument document = documents.findById(id)
                .filter(LegalDocument::isActive)
                .orElseThrow(() -> new NotFoundException("Active legal document not found"));
        if (!applies(document, user)) throw new ForbiddenException("Legal document does not apply to this role");
        if (!acceptances.existsByUserIdAndLegalDocumentId(user.getId(), id)) {
            acceptances.save(LegalAcceptance.builder().user(user).legalDocument(document)
                    .ipAddress(request.getRemoteAddr()).userAgent(request.getHeader("User-Agent")).build());
        }
        return map(document, true);
    }
    @Transactional(readOnly = true)
    public LegalStatusResponse status(User user) {
        List<LegalDocumentResponse> all = active(user);
        return new LegalStatusResponse(all.stream().allMatch(LegalDocumentResponse::accepted),
                all.stream().filter(item -> !item.accepted()).toList(),
                all.stream().filter(LegalDocumentResponse::accepted).toList());
    }
    @Transactional(readOnly = true)
    public void requireAccepted(User user) {
        if (!parameters.requireLegalAcceptance() || user.getRole() == Role.ADMIN || user.getRole() == Role.VERIFIER) return;
        if (!status(user).complete()) throw new ConflictException("Accept required legal documents before continuing");
    }
    @Transactional(readOnly = true)
    public List<LegalDocumentResponse> adminList(User admin) {
        requireAdmin(admin);
        return documents.findAllByOrderByCodeAscCreatedAtDesc().stream().map(item -> map(item, false)).toList();
    }
    @Transactional
    public LegalDocumentResponse create(LegalDocumentRequest request, User admin) {
        requireAdmin(admin);
        if (request.active()) deactivateCurrent(request.code(), request.roleTarget());
        return map(documents.save(LegalDocument.builder().code(request.code().trim()).title(request.title().trim())
                .version(request.version().trim()).roleTarget(request.roleTarget()).content(request.content().trim())
                .active(request.active()).build()), false);
    }
    @Transactional
    public LegalDocumentResponse update(UUID id, LegalDocumentRequest request, User admin) {
        requireAdmin(admin);
        LegalDocument item = documents.findById(id).orElseThrow(() -> new NotFoundException("Legal document not found"));
        if (request.active()) deactivateCurrent(request.code(), request.roleTarget());
        item.setCode(request.code().trim()); item.setTitle(request.title().trim());
        item.setVersion(request.version().trim()); item.setRoleTarget(request.roleTarget());
        item.setContent(request.content().trim()); item.setActive(request.active());
        return map(item, false);
    }
    @Transactional
    public LegalDocumentResponse activate(UUID id, User admin) {
        requireAdmin(admin);
        LegalDocument item = documents.findById(id).orElseThrow(() -> new NotFoundException("Legal document not found"));
        deactivateCurrent(item.getCode(), item.getRoleTarget());
        item.setActive(true);
        return map(item, false);
    }
    private List<LegalDocument> applicable(User user) {
        return documents.findByActiveTrueOrderByCodeAsc().stream().filter(item -> applies(item, user)).toList();
    }
    private boolean applies(LegalDocument item, User user) {
        return item.getRoleTarget() == LegalRoleTarget.ALL
                || item.getRoleTarget().name().equals(user.getRole().name());
    }
    private void deactivateCurrent(String code, LegalRoleTarget role) {
        documents.findByCodeAndRoleTargetAndActiveTrue(code, role).ifPresent(item -> item.setActive(false));
    }
    private void requireAdmin(User user) { if (user.getRole() != Role.ADMIN) throw new ForbiddenException("Admin role is required"); }
    private LegalDocumentResponse map(LegalDocument item, boolean accepted) {
        return new LegalDocumentResponse(item.getId(), item.getCode(), item.getTitle(), item.getVersion(),
                item.getRoleTarget(), item.getContent(), item.isActive(), accepted, item.getCreatedAt());
    }
}
