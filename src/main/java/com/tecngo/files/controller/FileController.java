package com.tecngo.files.controller;

import com.tecngo.files.dto.FileUploadResponse;
import com.tecngo.files.service.FileStorage;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
public class FileController {
    private final FileStorage storage;
    private final UserRepository users;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse upload(@RequestPart("file") MultipartFile file,
                                     @RequestParam(defaultValue = "DOCUMENT") FileKind kind) {
        var stored = storage.store(file, kind == FileKind.PROFILE);
        return new FileUploadResponse(stored.fileName(), stored.contentType(), stored.size(),
                "/v1/files/" + stored.fileName());
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> download(@PathVariable String fileName,
                                             @AuthenticationPrincipal User viewer) {
        String url = "/v1/files/" + fileName;
        boolean privateEvidence = fileName.startsWith("private-")
                || users.existsByDocumentPhotoUrlOrCertificatePhotoUrl(url, url);
        if (privateEvidence && !canViewPrivate(viewer, url)) {
            throw new ForbiddenException("This evidence file is private");
        }
        Resource resource = storage.load(fileName);
        MediaType type = fileName.endsWith(".pdf") ? MediaType.APPLICATION_PDF
                : fileName.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }

    private boolean canViewPrivate(User viewer, String url) {
        if (viewer == null) return false;
        if (viewer.getRole() == Role.ADMIN) return true;
        return url.equals(viewer.getDocumentPhotoUrl()) || url.equals(viewer.getCertificatePhotoUrl());
    }
}
