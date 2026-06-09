package com.trace.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.trace.config.MinioConfig;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @PostConstruct
    public void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioConfig.getBucket()).build());
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minioConfig.getBucket()).build());
                log.info("Created MinIO bucket: {}", minioConfig.getBucket());
            } else {
                log.info("MinIO bucket already exists: {}", minioConfig.getBucket());
            }
        } catch (Exception e) {
            log.error("Failed to ensure MinIO bucket exists", e);
        }
    }

    /**
     * 生成 PDF 并上传到 MinIO，返回访问 URL
     */
    public String generateAndUpload(String title, String content) {
        try {
            byte[] pdfBytes = generatePdf(title, content);
            String objectName = generateObjectName(title);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(objectName)
                            .stream(new ByteArrayInputStream(pdfBytes), pdfBytes.length, -1)
                            .contentType("application/pdf")
                            .build()
            );

            String url = minioConfig.getEndpoint() + "/" + minioConfig.getBucket() + "/" + objectName;
            log.info("PDF uploaded: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Failed to generate and upload PDF: title={}", title, e);
            throw new RuntimeException("PDF 生成失败: " + e.getMessage(), e);
        }
    }

    private byte[] generatePdf(String title, String content) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph(title).setFontSize(18).setBold());
        document.add(new Paragraph(" "));

        // 按行写入内容
        for (String line : content.split("\n")) {
            if (line.startsWith("# ")) {
                document.add(new Paragraph(line.substring(2)).setFontSize(16).setBold());
            } else if (line.startsWith("## ")) {
                document.add(new Paragraph(line.substring(3)).setFontSize(14).setBold());
            } else if (line.startsWith("### ")) {
                document.add(new Paragraph(line.substring(4)).setFontSize(12).setBold());
            } else if (!line.isBlank()) {
                document.add(new Paragraph(line).setFontSize(10));
            }
        }

        document.add(new Paragraph(" "));
        document.add(new Paragraph("由 Trace AI 生成").setFontSize(8).setItalic());

        document.close();
        return baos.toByteArray();
    }

    private String generateObjectName(String title) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String safeName = title.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_");
        if (safeName.length() > 50) safeName = safeName.substring(0, 50);
        return "reports/" + timestamp + "_" + safeName + ".pdf";
    }
}
