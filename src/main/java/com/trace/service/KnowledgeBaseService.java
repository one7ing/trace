package com.trace.service;

import com.trace.entity.KnowledgeBase;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface KnowledgeBaseService {
    List<KnowledgeBase> uploadFile(Long userId, MultipartFile file, String knowledgeType);
    List<KnowledgeBase> search(Long userId, String query, String knowledgeType, int limit);
    List<KnowledgeBase> list(Long userId, String knowledgeType);
    void deleteById(Long userId, Long id);
    void clearUserKnowledge(Long userId);
    String getFileContent(Long userId, String fileName);
    void updateFileContent(Long userId, String fileName, String newContent);
    void renameFile(Long userId, String oldFileName, String newFileName);
    void deleteByFileName(Long userId, String fileName);
}
