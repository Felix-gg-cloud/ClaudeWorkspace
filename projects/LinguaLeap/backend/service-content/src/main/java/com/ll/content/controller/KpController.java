package com.ll.content.controller;

import com.ll.common.dto.ApiResponse;
import com.ll.content.entity.KnowledgePoint;
import com.ll.content.service.KpService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content")
public class KpController {

    private final KpService kpService;

    public KpController(KpService kpService) {
        this.kpService = kpService;
    }

    @GetMapping("/banks/{bankId}/kps")
    public ApiResponse<Page<KnowledgePoint>> list(
            @PathVariable Long bankId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(kpService.list(bankId, type, keyword, difficulty, page, size));
    }

    @GetMapping("/kps/{id}")
    public ApiResponse<KnowledgePoint> getById(@PathVariable Long id) {
        return ApiResponse.ok(kpService.getById(id));
    }

    @PostMapping("/banks/{bankId}/kps")
    public ApiResponse<KnowledgePoint> create(@PathVariable Long bankId, @RequestBody KnowledgePoint kp) {
        return ApiResponse.ok(kpService.create(bankId, kp));
    }

    @PutMapping("/kps/{id}")
    public ApiResponse<KnowledgePoint> update(@PathVariable Long id, @RequestBody KnowledgePoint kp) {
        return ApiResponse.ok(kpService.update(id, kp));
    }

    @DeleteMapping("/kps/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        kpService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/banks/{bankId}/kps/batch")
    public ApiResponse<List<KnowledgePoint>> batchImport(@PathVariable Long bankId, @RequestBody List<KnowledgePoint> kps) {
        return ApiResponse.ok(kpService.batchImport(bankId, kps));
    }
}
