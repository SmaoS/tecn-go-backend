package com.tecngo.files.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public interface FileStorage {
    StoredFile store(MultipartFile file, boolean publicAccess);
    default StoredFile store(MultipartFile file, boolean publicAccess, String folder,
                             Set<String> allowedTypes) {
        return store(file, publicAccess);
    }
    default void delete(String publicId) {
        throw new UnsupportedOperationException("Delete is not supported");
    }
    Resource load(String fileName);

    record StoredFile(
            String fileName, String contentType, long size, String accessUrl,
            String secureUrl, String publicId, boolean publicAccess
    ) {}
}
