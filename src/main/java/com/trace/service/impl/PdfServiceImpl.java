package com.trace.service.impl;

import com.lowagie.text.pdf.BaseFont;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * PDF 生成服务 —— 使用 Thymeleaf 模板 + Flying Saucer（OpenPDF）生成 PDF。
 *
 * <p>流程：AI 生成的 Markdown 内容 → 转 HTML → Thymeleaf 模板渲染完整 HTML → Flying Saucer 转 PDF → 上传 MinIO</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final SpringTemplateEngine templateEngine;

    /** 中文字体路径（文件系统绝对路径），初始化时解析 */
    private String chineseFontPath;

    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)");
    private static final Pattern CODE_PATTERN = Pattern.compile("`([^`]+)`");

    // ────────── 字体初始化 ──────────

    /**
     * 解析中文字体路径。优先级：
     * <ol>
     *   <li>classpath:/fonts/ 下的 .ttf/.ttc 文件</li>
     *   <li>Windows 系统字体目录</li>
     *   <li>回退 null（Flying Saucer 使用内置 STSong-Light）</li>
     * </ol>
     * <p>对 .ttc 字体自动追加 {@code ,0} 索引。</p>
     */
    private String resolveFontPath() {
        // 1. classpath:/fonts/
        String[] classpathFonts = {
                "fonts/SimSun.ttf", "fonts/SimSun.ttc",
                "fonts/simsun.ttf", "fonts/simsun.ttc",
                "fonts/msyh.ttf", "fonts/msyh.ttc"
        };
        for (String cp : classpathFonts) {
            try {
                ClassPathResource res = new ClassPathResource(cp);
                if (res.exists()) {
                    Path tmp = Files.createTempFile("trace_font_",
                            cp.substring(cp.lastIndexOf('.')));
                    tmp.toFile().deleteOnExit();
                    Files.copy(res.getInputStream(), tmp,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    String path = tmp.toAbsolutePath().toString();
                    if (path.endsWith(".ttc")) path += ",0";
                    log.info("从classpath加载字体: {} → {}", cp, path);
                    return path;
                }
            } catch (Exception ignored) { /* try next */ }
        }

        // 2. Windows 系统字体
        String[] systemFonts = {
                "c:/windows/fonts/simsun.ttc",
                "c:/windows/fonts/simsun.ttf",
                "c:/windows/fonts/msyh.ttc",
                "c:/windows/fonts/msyh.ttf"
        };
        for (String sf : systemFonts) {
            try {
                if (Files.exists(Path.of(sf))) {
                    String path = sf;
                    if (path.endsWith(".ttc")) path += ",0";
                    log.info("使用系统字体: {}", path);
                    return path;
                }
            } catch (Exception ignored) { /* try next */ }
        }

        log.warn("未找到中文字体文件，PDF可能无法正确渲染中文字符");
        return null;
    }

    @PostConstruct
    void initFont() {
        this.chineseFontPath = resolveFontPath();
    }

    // ────────── 公开方法 ──────────

    @PostConstruct
    public void ensureBucketExists() {
        try {
            String bucket = minioConfig.getBucket();
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO存储桶已创建: {}", bucket);
            }
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
                    SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
            log.info("MinIO存储桶已设置公开读策略: {}", bucket);
        } catch (Exception e) {
            log.error("确保MinIO存储桶存在失败: PdfServiceImpl.ensureBucketExists", e);
        }
    }

    @Override
    public String generateAndUpload(String title, String content) {
        try {
            byte[] pdfBytes = generatePdf(title, content);
            String objectName = generateObjectName(title);
            //上传文件
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket()).object(objectName)
                    .stream(new ByteArrayInputStream(pdfBytes), pdfBytes.length, -1)
                    .contentType("application/pdf").build());
            String url = minioConfig.getEndpoint() + "/" + minioConfig.getBucket() + "/" + objectName;
            log.info("PDF已上传: {}", url);
            return url;
        } catch (Exception e) {
            log.error("PDF生成并上传失败: title={}, 错误位置=PdfServiceImpl.generateAndUpload", title, e);
            throw new RuntimeException("PDF 生成失败: " + e.getMessage(), e);
        }
    }

    // ────────── PDF 生成核心 ──────────

    /**
     * Markdown → HTML body → Thymeleaf 模板渲染完整 HTML → Flying Saucer 渲染 PDF。
     */
    private byte[] generatePdf(String title, String markdownContent) throws Exception {
        // 1. Markdown → HTML body
        String contentHtml = markdownToHtml(markdownContent);

        // 2. Thymeleaf 模板渲染
        Context ctx = new Context();
        ctx.setVariable("title", title);
        ctx.setVariable("contentHtml", contentHtml);
        String fullHtml = templateEngine.process("report", ctx);

        // 3. Flying Saucer HTML → PDF
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();

        if (chineseFontPath != null) {
            try {
                renderer.getFontResolver().addFont(
                        chineseFontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                log.debug("PDF字体已注册: {}", chineseFontPath);
            } catch (Exception e) {
                log.warn("PDF字体注册失败 {}: {}, 错误位置=PdfServiceImpl.generatePdf", chineseFontPath, e.getMessage());
            }
        }

        renderer.setDocumentFromString(fullHtml);
        renderer.layout();
        renderer.createPDF(baos);
        baos.close();

        return baos.toByteArray();
    }

    // ────────── Markdown → HTML 转换 ──────────

    /**
     * 将 AI 生成的 Markdown 文本转为 HTML。
     * 支持：标题(#)、无序列表(- *)、有序列表(1.)、代码块(```)、引用(&gt;)、分隔线(---)、
     * 加粗(**)、斜体(*)、行内代码(`)。
     */
    private String markdownToHtml(String md) {
        StringBuilder html = new StringBuilder(4096);
        String[] lines = md.replace("\r\n", "\n").replace("\r", "\n").split("\n");

        boolean inCodeBlock = false;
        StringBuilder codeBuf = new StringBuilder();
        // 列表状态：null=不在列表中, "ul"/"ol"=当前列表类型
        String openList = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // ── 代码块 ──
            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    html.append("<pre><code>")
                            .append(escapeHtml(codeBuf.toString()))
                            .append("</code></pre>\n");
                    codeBuf.setLength(0);
                    inCodeBlock = false;
                } else {
                    openList = closeList(html, openList);
                    inCodeBlock = true;
                }
                continue;
            }
            if (inCodeBlock) {
                if (!codeBuf.isEmpty()) codeBuf.append("\n");
                codeBuf.append(line);
                continue;
            }

            // ── 空行 → 结束列表 ──
            if (line.trim().isEmpty()) {
                openList = closeList(html, openList);
                continue;
            }

            String trimmed = line.trim();

            // ── 标题 ──
            if (trimmed.startsWith("### ")) {
                openList = closeList(html, openList);
                html.append("<h4>").append(inlineFormat(trimmed.substring(4))).append("</h4>\n");
                continue;
            }
            if (trimmed.startsWith("## ")) {
                openList = closeList(html, openList);
                html.append("<h3>").append(inlineFormat(trimmed.substring(3))).append("</h3>\n");
                continue;
            }
            if (trimmed.startsWith("# ")) {
                openList = closeList(html, openList);
                html.append("<h2>").append(inlineFormat(trimmed.substring(2))).append("</h2>\n");
                continue;
            }

            // ── 分隔线 ──
            if (trimmed.matches("^-{3,}$") || trimmed.matches("^\\*{3,}$")) {
                openList = closeList(html, openList);
                html.append("<hr/>\n");
                continue;
            }

            // ── 引用块 ──
            if (trimmed.startsWith("> ")) {
                openList = closeList(html, openList);
                html.append("<blockquote><p>")
                        .append(inlineFormat(trimmed.substring(2)))
                        .append("</p></blockquote>\n");
                continue;
            }

            // ── 无序列表 ──
            if (trimmed.matches("^[-*]\\s+.+")) {
                String item = trimmed.replaceFirst("^[-*]\\s+", "");
                if (!"ul".equals(openList)) {
                    openList = closeList(html, openList);
                    html.append("<ul>\n");
                    openList = "ul";
                }
                html.append("<li>").append(inlineFormat(item)).append("</li>\n");
                continue;
            }

            // ── 有序列表 ──
            if (trimmed.matches("^\\d+\\.\\s+.+")) {
                String item = trimmed.replaceFirst("^\\d+\\.\\s+", "");
                if (!"ol".equals(openList)) {
                    openList = closeList(html, openList);
                    html.append("<ol>\n");
                    openList = "ol";
                }
                html.append("<li>").append(inlineFormat(item)).append("</li>\n");
                continue;
            }

            // ── 普通段落 ──
            openList = closeList(html, openList);
            html.append("<p>").append(inlineFormat(trimmed)).append("</p>\n");
        }

        // 收尾
        if (inCodeBlock && !codeBuf.isEmpty()) {
            html.append("<pre><code>")
                    .append(escapeHtml(codeBuf.toString()))
                    .append("</code></pre>\n");
        }
        closeList(html, openList);

        return html.toString();
    }

    /** 关闭列表标签，返回 null */
    private String closeList(StringBuilder html, String openList) {
        if (openList != null) {
            html.append("</").append(openList).append(">\n");
        }
        return null;
    }

    /** 处理行内格式：加粗 **text**、斜体 *text*、行内代码 `text` */
    private String inlineFormat(String text) {
        String escaped = escapeHtml(text);
        escaped = BOLD_PATTERN.matcher(escaped).replaceAll("<strong>$1</strong>");
        escaped = ITALIC_PATTERN.matcher(escaped).replaceAll("<em>$1</em>");
        escaped = CODE_PATTERN.matcher(escaped).replaceAll("<code>$1</code>");
        return escaped;
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // ────────── 辅助 ──────────

    private String generateObjectName(String title) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String safeName = title.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_");
        if (safeName.length() > 50) safeName = safeName.substring(0, 50);
        return "reports/" + timestamp + "_" + safeName + ".pdf";
    }
}
