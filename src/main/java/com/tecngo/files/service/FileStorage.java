package com.tecngo.files.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    StoredFile store(MultipartFile file, boolean publicAccess);
    Resource load(String fileName);

    record StoredFile(String fileName, String contentType, long size) {}
}
