package com.tecngo.files.controller;

import com.tecngo.files.dto.FileUploadResponse;
import com.tecngo.files.service.FileStorage;
import com.tecngo.content_moderation.entity.*;
import com.tecngo.content_moderation.repository.ContentAssetRepository;
import com.tecngo.content_moderation.service.ContentModerationService;
import com.tecngo.content_moderation.service.ModeratedFileService;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import com.tecngo.service_evidence.repository.ServiceEvidenceRepository;
import com.tecngo.payment_proofs.repository.PaymentProofRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Set;

@RestController
@RequestMapping({"/files", "/v1/files"})
@RequiredArgsConstructor
@Slf4j
public class FileController {
    private final FileStorage storage;
    private final UserRepository users;
    private final ServiceEvidenceRepository evidences;
    private final PaymentProofRepository paymentProofs;
    private final ContentAssetRepository assets;
    private final ContentModerationService contentModeration;
    private final ModeratedFileService moderatedFiles;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse upload(@RequestPart("file") MultipartFile file,
                                     @RequestParam(defaultValue = "DOCUMENT") FileKind kind,
                                     @AuthenticationPrincipal User user) {
        String folder = switch (kind) {
            case PROFILE -> "tecngo/profiles";
            case DOCUMENT -> "tecngo/documents";
            case CERTIFICATE -> "tecngo/certificates";
        };
        ContentAssetKind assetKind = switch (kind) {
            case PROFILE -> ContentAssetKind.PROFILE;
            case DOCUMENT -> ContentAssetKind.DOCUMENT;
            case CERTIFICATE -> ContentAssetKind.CERTIFICATE;
        };
        var result = moderatedFiles.store(file, folder,
                Set.of("image/jpeg", "image/png", "application/pdf"), assetKind, user);
        var stored = result.stored();
        return new FileUploadResponse(stored.fileName(), stored.contentType(), stored.size(),
                stored.accessUrl(), null, stored.publicId(), result.asset().getId(),
                result.asset().getModerationStatus());
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> download(@PathVariable String fileName,
                                             @AuthenticationPrincipal User viewer) {
        String url = "/v1/files/" + fileName;
        var asset = assets.findByFileUrl(url).orElse(null);
        if (asset != null && !contentModeration.canDownload(asset, viewer)) {
            log.warn("Private file denied: fileName={}, assetId={}, kind={}, moderationStatus={}, viewerId={}, viewerRole={}",
                    fileName, asset.getId(), asset.getKind(), asset.getModerationStatus(),
                    viewer == null ? null : viewer.getId(), viewer == null ? null : viewer.getRole());
            throw new ForbiddenException("This file is private or has not been approved");
        }
        boolean privateEvidence = fileName.startsWith("private-")
                || users.existsByProfilePhotoUrl(url)
                || users.existsByDocumentPhotoUrlOrCertificatePhotoUrl(url, url);
        if (asset == null && privateEvidence && !canViewPrivate(viewer, url)) {
            log.warn("Private file denied without content asset: fileName={}, viewerId={}, viewerRole={}",
                    fileName, viewer == null ? null : viewer.getId(), viewer == null ? null : viewer.getRole());
            throw new ForbiddenException("This evidence file is private");
        }
        Resource resource = storage.load(fileName);
        String resolvedUrl;
        try {
            resolvedUrl = resource.getURL().toString();
        } catch (java.io.IOException exception) {
            resolvedUrl = fileName;
        }
        MediaType type = resolvedUrl.contains("/raw/") || resolvedUrl.endsWith(".pdf")
                ? MediaType.APPLICATION_PDF
                : resolvedUrl.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }

    private boolean canViewPrivate(User viewer, String url) {
        if (viewer == null) return false;
        if (viewer.getRole() == Role.ADMIN || viewer.getRole() == Role.VERIFIER) return true;
        var profileOwner = users.findByProfilePhotoUrl(url).orElse(null);
        if (profileOwner != null) {
            return profileOwner.getId().equals(viewer.getId())
                    || profileOwner.isProfilePhotoFaceValidated();
        }
        if (url.equals(viewer.getDocumentPhotoUrl()) || url.equals(viewer.getCertificatePhotoUrl())) return true;
        var evidence = evidences.findByFileUrl(url).orElse(null);
        if (evidence != null) {
            var request = evidence.getServiceRequest();
            return request.getClient().getId().equals(viewer.getId())
                    || request.getTechnician() != null && request.getTechnician().getId().equals(viewer.getId());
        }
        var proof = paymentProofs.findByFileUrl(url).orElse(null);
        if (proof != null) {
            var request = proof.getServiceRequest();
            return request.getClient().getId().equals(viewer.getId())
                    || request.getTechnician() != null && request.getTechnician().getId().equals(viewer.getId());
        }
        return false;
    }
}
