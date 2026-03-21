package com.eqa.controller;

import com.eqa.entity.SrsRecord;
import com.eqa.entity.User;
import com.eqa.repository.UserRepository;
import com.eqa.service.SrsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/srs")
public class SrsController {

    private final SrsService srsService;
    private final UserRepository userRepo;

    public SrsController(SrsService srsService, UserRepository userRepo) {
        this.srsService = srsService;
        this.userRepo = userRepo;
    }

    /**
     * 获取今天需要复习的卡片
     */
    @GetMapping("/due")
    public List<Map<String, Object>> dueCards(Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        return srsService.getDueCards(user.getId()).stream()
                .map(srsService::toMap)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有 SRS 记录
     */
    @GetMapping("/all")
    public List<Map<String, Object>> allRecords(Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        return srsService.getAllRecords(user.getId()).stream()
                .map(srsService::toMap)
                .collect(Collectors.toList());
    }

    /**
     * 提交复习结果（SM-2 算法）
     */
    @PostMapping("/review")
    public ResponseEntity<?> review(@RequestBody ReviewRequest req, Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        SrsRecord record = srsService.review(user.getId(), req.contentItemCode(), req.quality());
        return ResponseEntity.ok(srsService.toMap(record));
    }

    /**
     * 批量复习
     */
    @PostMapping("/review-batch")
    public ResponseEntity<?> reviewBatch(@RequestBody List<ReviewRequest> reqs, Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        List<Map<String, Object>> results = new ArrayList<>();
        for (ReviewRequest req : reqs) {
            SrsRecord record = srsService.review(user.getId(), req.contentItemCode(), req.quality());
            results.add(srsService.toMap(record));
        }
        return ResponseEntity.ok(results);
    }

    public record ReviewRequest(String contentItemCode, int quality) {}
}
