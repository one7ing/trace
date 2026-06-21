package com.trace.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trace.dto.ApiResponse;
import com.trace.dto.DiaryRequest;
import com.trace.entity.Diary;
import com.trace.service.DiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/diary") @RequiredArgsConstructor
public class DiaryController {
    private final DiaryService diaryService;
    @PostMapping
    public ResponseEntity<ApiResponse<Diary>> create(@AuthenticationPrincipal Long userId, @Valid @RequestBody DiaryRequest req){
        return ResponseEntity
                .ok(ApiResponse
                        .success("日记创建成功", diaryService.create(userId, req)));
    }
    @GetMapping
    public ResponseEntity<ApiResponse<IPage<Diary>>> list(@AuthenticationPrincipal Long userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) { return ResponseEntity.ok(ApiResponse.success(diaryService.list(userId, page, size))); }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Diary>> get(@PathVariable Long id) { return ResponseEntity.ok(ApiResponse.success(diaryService.getById(id))); }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Diary>> update(@PathVariable Long id, @Valid @RequestBody DiaryRequest req) { return ResponseEntity.ok(ApiResponse.success("更新成功", diaryService.update(id, req))); }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) { diaryService.delete(id); return ResponseEntity.ok(ApiResponse.success("已删除", null)); }
}
