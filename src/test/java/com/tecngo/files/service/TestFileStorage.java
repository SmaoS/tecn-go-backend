package com.tecngo.files.service;

import com.tecngo.shared.exception.NotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Profile("test")
public class TestFileStorage implements FileStorage {
    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "application/pdf");

    @Override
    public StoredFile store(MultipartFile file, boolean publicAccess) {
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (file.isEmpty()) throw new IllegalArgumentException("File is required");
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPG, PNG and PDF files are allowed");
        }
        String fileName = (publicAccess ? "public-" : "private-") + UUID.randomUUID();
        String url = "/v1/files/" + fileName;
        return new StoredFile(fileName, contentType, file.getSize(), url, url, fileName, publicAccess);
    }

    @Override
    public StoredFile store(MultipartFile file, boolean publicAccess, String folder,
                            Set<String> allowedTypes) {
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!allowedTypes.contains(contentType)) throw new IllegalArgumentException("File type is not allowed");
        String fileName = (publicAccess ? "public-" : "private-") + UUID.randomUUID();
        String url = "/v1/files/" + fileName;
        return new StoredFile(fileName, contentType, file.getSize(),
                url, url, folder + "/" + UUID.randomUUID(), publicAccess);
    }

    @Override
    public void delete(String publicId) {
        // No-op test storage.
    }

    @Override
    public Resource load(String fileName) {
        if (fileName.isBlank()) throw new NotFoundException("File not found");
        return new ByteArrayResource(new byte[]{1});
    }
}
