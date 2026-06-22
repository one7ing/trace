package com.trace.controller;

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

@Tag(name = "知识库管理", description = "文件上传、向量检索、混合检索、题库导入")
@RestController
@RequestMapping("/api/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBaseController {
    private final KnowledgeBaseService kbService;
    private final QuestionBankService questionBankService;

    @Operation(summary = "导入题库", description = "从题库文件解析 Q&A 并写入 question_bank 表")
    @PostMapping("/import-bank")
    public ResponseEntity<ApiResponse<Integer>> importBank() {
        int count = questionBankService.importQuestionBank();
        return ResponseEntity.ok(ApiResponse.success("题库导入完成，共 " + count + " 条", count));
    }

    @Operation(summary = "检查知识库文件", description = "检查当前用户是否有知识库文件")
    @GetMapping("/has-files")
    public ResponseEntity<ApiResponse<Boolean>> hasFiles(
            @AuthenticationPrincipal Long userId) {
        List<KnowledgeBase> all = kbService.list(userId, null);
        return ResponseEntity.ok(ApiResponse.success(!all.isEmpty()));
    }

    /** 上传文件 */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<List<KnowledgeBase>>> upload(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "USER") String knowledgeType) {
        return ResponseEntity.ok(ApiResponse.success("上传成功", kbService.uploadFile(userId, file, knowledgeType)));
    }

    /** 语义搜索 */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<KnowledgeBase>>> search(
            @AuthenticationPrincipal Long userId,
            @RequestParam String query,
            @RequestParam(defaultValue = "USER") String knowledgeType,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.success(kbService.search(userId, query, knowledgeType, limit)));
    }

    /** 列表 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<KnowledgeBase>>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "USER") String knowledgeType) {
        return ResponseEntity.ok(ApiResponse.success(kbService.list(userId, knowledgeType)));
    }

    /** 删除单条 */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId, @PathVariable Long id) {
        kbService.deleteById(userId, id);
        return ResponseEntity.ok(ApiResponse.success("已删除", null));
    }

    /** 查看文件全文 */
    @GetMapping("/file")
    public ResponseEntity<ApiResponse<Map<String, String>>> getFile(
            @AuthenticationPrincipal Long userId,
            @RequestParam String fileName) {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("fileName", fileName, "content", kbService.getFileContent(userId, fileName))));
    }

    /** 编辑文件内容 */
    @PutMapping("/file")
    public ResponseEntity<ApiResponse<Void>> updateFile(
            @AuthenticationPrincipal Long userId,
            @RequestParam String fileName,
            @RequestBody Map<String, String> body) {
        kbService.updateFileContent(userId, fileName, body.get("content"));
        return ResponseEntity.ok(ApiResponse.success("已更新", null));
    }

    /** 重命名文件 */
    @PutMapping("/rename")
    public ResponseEntity<ApiResponse<Void>> rename(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        kbService.renameFile(userId, body.get("oldName"), body.get("newName"));
        return ResponseEntity.ok(ApiResponse.success("已重命名", null));
    }

    /** 按文件名删除全部文档 */
    @DeleteMapping("/file")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @AuthenticationPrincipal Long userId, @RequestParam String fileName) {
        kbService.deleteByFileName(userId, fileName);
        return ResponseEntity.ok(ApiResponse.success("已删除", null));
    }

    /** 混合检索（向量 0.7 + 全文 0.3） */
    @GetMapping("/hybrid-search")
    public ResponseEntity<ApiResponse<List<org.springframework.ai.document.Document>>> hybridSearch(
            @AuthenticationPrincipal Long userId,
            @RequestParam String query,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "10") int topK) {
        return ResponseEntity.ok(ApiResponse.success(
                kbService.hybridSearch(userId, query, category, topK)));
    }

    /** 清空用户知识库 */
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clear(@AuthenticationPrincipal Long userId) {
        kbService.clearUserKnowledge(userId);
        return ResponseEntity.ok(ApiResponse.success("已清空", null));
    }
}
