package com.trace.service.impl;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.trace.config.MinioConfig;
import com.trace.service.PdfService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
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
public class PdfServiceImpl implements PdfService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    private PdfFont chineseFont;

    @Override
    @PostConstruct
    public void ensureBucketExists() {
        try {
            String bucket = minioConfig.getBucket();
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            }
            // 设置桶策略为公开读，允许浏览器直接访问 PDF
            String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);
            minioClient.setBucketPolicy(
                    io.minio.SetBucketPolicyArgs.builder()
                            .bucket(bucket).config(policy).build());
            log.info("Set public-read policy on bucket: {}", bucket);
        } catch (Exception e) {
            log.error("Failed to ensure MinIO bucket exists", e);
        }
    }

    @Override
    public String generateAndUpload(String title, String content) {
        try {
            byte[] pdfBytes = generatePdf(title, content);
            String objectName = generateObjectName(title);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket()).object(objectName)
                    .stream(new ByteArrayInputStream(pdfBytes), pdfBytes.length, -1)
                    .contentType("application/pdf").build());
            String url = minioConfig.getEndpoint() + "/" + minioConfig.getBucket() + "/" + objectName;
            log.info("PDF uploaded: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Failed to generate and upload PDF: title={}", title, e);
            throw new RuntimeException("PDF 生成失败: " + e.getMessage(), e);
        }
    }

    private synchronized PdfFont getChineseFont() throws Exception {
        if (chineseFont == null) {
            try {
                // 尝试 Windows SimSun 字体，IDENTITY_H 支持所有 Unicode
                chineseFont = PdfFontFactory.createFont(
                        "c:/windows/fonts/simsun.ttc,0",
                        PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            } catch (Exception e) {
                try {
                    // 尝试 STSong 用 IDENTITY_H（不是 UniGB-UCS2-H）
                    chineseFont = PdfFontFactory.createFont(
                            "STSong-Light", PdfEncodings.IDENTITY_H,
                            PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                } catch (Exception e2) {
                    // 最后回退
                    log.warn("No Chinese font found, PDF may not render correctly");
                    chineseFont = PdfFontFactory.createFont();
                }
            }
        }
        return chineseFont;
    }

    private byte[] generatePdf(String title, String content) throws Exception {
        PdfFont font = getChineseFont();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
        Document document = new Document(pdf);

        document.add(new Paragraph(title).setFont(font).setFontSize(18));

        for (String line : content.split("\n")) {
            if (line.startsWith("# ")) {
                document.add(new Paragraph(line.substring(2)).setFont(font).setFontSize(16));
            } else if (line.startsWith("## ")) {
                document.add(new Paragraph(line.substring(3)).setFont(font).setFontSize(14));
            } else if (line.startsWith("### ")) {
                document.add(new Paragraph(line.substring(4)).setFont(font).setFontSize(12));
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                document.add(new Paragraph("  " + line).setFont(font).setFontSize(9));
            } else if (!line.isBlank()) {
                document.add(new Paragraph(line).setFont(font).setFontSize(9));
            }
        }
        document.add(new Paragraph(" "));
        document.add(new Paragraph("由 Trace AI 生成").setFont(font).setFontSize(8));
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
