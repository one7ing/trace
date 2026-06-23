package com.trace.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trace.dto.ApiResponse;
import com.trace.entity.KnowledgeBase;
import com.trace.service.QuestionBankService;
import com.trace.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "知识库管理", description = "文件上传、检索、条目管理")
@RestController
@RequestMapping("/api/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBaseController {
    private final KnowledgeBaseService kbService;
    private final QuestionBankService questionBankService;

    // ===== 上传 =====

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<KnowledgeBase>> upload(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "专业知识问答") String category) {
        return ResponseEntity.ok(ApiResponse.success("上传成功",
                kbService.uploadFile(userId, file, category)));
    }

    @PostMapping("/import-bank")
    public ResponseEntity<ApiResponse<Integer>> importBank() {
        int count = questionBankService.importQuestionBank();
        return ResponseEntity.ok(ApiResponse.success("题库导入完成，共 " + count + " 条", count));
    }

    // ===== 查询 =====

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<KnowledgeBase>>> search(
            @AuthenticationPrincipal Long userId,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(kbService.search(userId, query, limit)));
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<IPage<KnowledgeBase>>> listItems(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                kbService.listItems(userId, category, page, size)));
    }

    // ===== 文件操作 =====

    @GetMapping("/file")
    public ResponseEntity<ApiResponse<Map<String, String>>> getFile(
            @AuthenticationPrincipal Long userId,
            @RequestParam String fileName) {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "fileName", fileName,
                "content", kbService.getFileContent(userId, fileName))));
    }

    @PutMapping("/file")
    public ResponseEntity<ApiResponse<Void>> updateFile(
            @AuthenticationPrincipal Long userId,
            @RequestParam String fileName,
            @RequestBody Map<String, String> body) {
        kbService.updateFileContent(userId, fileName, body.get("content"));
        return ResponseEntity.ok(ApiResponse.success("已更新", null));
    }

    // ===== 条目管理 =====

    @PutMapping("/{id}/name")
    public ResponseEntity<ApiResponse<Void>> updateName(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        kbService.updateName(userId, id, body.get("name"));
        return ResponseEntity.ok(ApiResponse.success("名称已更新", null));
    }

    @DeleteMapping("/file")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @AuthenticationPrincipal Long userId,
            @RequestParam String fileName) {
        kbService.deleteByFileName(userId, fileName);
        return ResponseEntity.ok(ApiResponse.success("已删除", null));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clear(@AuthenticationPrincipal Long userId) {
        kbService.clearUserKnowledge(userId);
        return ResponseEntity.ok(ApiResponse.success("已清空", null));
    }
}
