package com.trace.controller;

import com.trace.dto.ApiResponse;
import com.trace.dto.DiaryRequest;
import com.trace.entity.Diary;
import com.trace.service.DiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    /**
     * 新建日记
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Diary>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody DiaryRequest request) {
        Diary diary = diaryService.create(userId, request);
        return ResponseEntity.ok(ApiResponse.success("日记创建成功", diary));
    }

    /**
     * 分页列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Diary>>> list(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<Diary> diaries = diaryService.list(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(diaries));
    }

    /**
     * 查看详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Diary>> getById(@PathVariable Long id) {
        Diary diary = diaryService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(diary));
    }

    /**
     * 编辑日记
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Diary>> update(
            @PathVariable Long id,
            @Valid @RequestBody DiaryRequest request) {
        Diary diary = diaryService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("日记更新成功", diary));
    }

    /**
     * 删除日记
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        diaryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("日记已删除", null));
    }
}
