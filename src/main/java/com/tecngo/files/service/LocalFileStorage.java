package com.tecngo.files.service;

import com.tecngo.shared.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorage implements FileStorage {
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "application/pdf");
    private final Path root;
    private final long maxBytes;

    public LocalFileStorage(@Value("${app.files.storage-path}") String storagePath,
                            @Value("${app.files.max-size-bytes}") long maxBytes) {
        this.root = Path.of(storagePath).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize file storage", exception);
        }
    }

    @Override
    public StoredFile store(MultipartFile file, boolean publicAccess) {
        if (file.isEmpty()) throw new IllegalArgumentException("File is required");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPG, PNG and PDF files are allowed");
        }
        if (file.getSize() > maxBytes) throw new IllegalArgumentException("File exceeds the configured maximum size");
        String extension = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "application/pdf" -> ".pdf";
            default -> throw new IllegalArgumentException("Unsupported file type");
        };
        String fileName = (publicAccess ? "public-" : "private-") + UUID.randomUUID() + extension;
        Path target = root.resolve(fileName).normalize();
        if (!target.getParent().equals(root)) throw new IllegalArgumentException("Invalid file name");
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(fileName, contentType, file.getSize());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store file", exception);
        }
    }

    @Override
    public Resource load(String fileName) {
        try {
            Path target = root.resolve(fileName).normalize();
            if (!target.getParent().equals(root)) throw new NotFoundException("File not found");
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable()) throw new NotFoundException("File not found");
            return resource;
        } catch (IOException exception) {
            throw new NotFoundException("File not found");
        }
    }
}
