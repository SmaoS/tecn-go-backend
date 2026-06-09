package com.tecngo.files.dto;

public record FileUploadResponse(String fileName, String contentType, long size, String url) {}
