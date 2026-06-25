package com.tecngo.files.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tecngo.shared.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.net.MalformedURLException;

@Service
@Profile("!test")
public class CloudinaryService implements FileStorage {
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "application/pdf");
    private final Cloudinary cloudinary;
    private final long maxBytes;

    public CloudinaryService(Cloudinary cloudinary,
                             @Value("${app.files.max-size-bytes}") long maxBytes) {
        this.cloudinary = cloudinary;
        this.maxBytes = maxBytes;
    }

    @Override
    public StoredFile store(MultipartFile file, boolean publicAccess) {
        return store(file, publicAccess, "tecngo", ALLOWED_TYPES);
    }

    @Override
    public StoredFile store(MultipartFile file, boolean publicAccess, String folder,
                            Set<String> allowedTypes) {
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (file.isEmpty()) throw new IllegalArgumentException("File is required");
        if (!allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException("File type is not allowed");
        }
        if (file.getSize() > maxBytes) throw new IllegalArgumentException("File exceeds the configured maximum size");
        try {
            String resourceType = contentType.startsWith("image/") ? "image" : "raw";
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", resourceType,
                    "type", publicAccess ? "upload" : "authenticated",
                    "use_filename", false,
                    "unique_filename", true
            ));
            String publicId = String.valueOf(result.get("public_id"));
            String secureUrl = String.valueOf(result.get("secure_url"));
            String format = result.get("format") == null ? "" : String.valueOf(result.get("format"));
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    String.join("|", resourceType, publicId, format).getBytes(StandardCharsets.UTF_8));
            String fileName = publicAccess ? publicId : "private-cloudinary-" + token;
            String accessUrl = publicAccess ? secureUrl : "/v1/files/" + fileName;
            return new StoredFile(fileName, contentType, file.getSize(), accessUrl,
                    secureUrl, publicId, publicAccess);
        } catch (IOException exception) {
            throw new IllegalStateException("Cloudinary upload failed", exception);
        }
    }

    @Override
    public void delete(String publicId) {
        delete(publicId, null);
    }

    @Override
    public void delete(String publicId, String contentType) {
        try {
            String resourceType = contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/")
                    ? "image" : "raw";
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "type", "authenticated"
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("Cloudinary delete failed", exception);
        }
    }

    @Override
    public Resource load(String fileName) {
        if (!fileName.startsWith("private-cloudinary-")) {
            throw new NotFoundException("Cloudinary file not found");
        }
        try {
            String token = fileName.substring("private-cloudinary-".length());
            String[] parts = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8)
                    .split("\\|", -1);
            if (parts.length != 3) throw new NotFoundException("Cloudinary file not found");
            var builder = cloudinary.url().resourceType(parts[0]).type("authenticated").signed(true);
            if (!parts[2].isBlank() && !"raw".equals(parts[0])) builder.format(parts[2]);
            return new UrlResource(builder.generate(parts[1]));
        } catch (IllegalArgumentException | MalformedURLException exception) {
            throw new NotFoundException("Cloudinary file not found");
        }
    }
}
