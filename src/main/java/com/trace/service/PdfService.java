package com.trace.service;

public interface PdfService {

    String generateAndUpload(String title, String content);

    void ensureBucketExists();
}
