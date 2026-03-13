package com.eqa.controller;

import com.eqa.entity.Chapter;
import com.eqa.entity.ChapterProgress;
import com.eqa.entity.User;
import com.eqa.repository.ChapterProgressRepository;
import com.eqa.repository.ChapterRepository;
import com.eqa.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chapters")
public class ChapterController {

    private final ChapterRepository chapterRepo;
    private final ChapterProgressRepository progressRepo;
    private final UserRepository userRepo;

    public ChapterController(ChapterRepository chapterRepo,
                             ChapterProgressRepository progressRepo,
                             UserRepository userRepo) {
        this.chapterRepo = chapterRepo;
        this.progressRepo = progressRepo;
        this.userRepo = userRepo;
    }

    @GetMapping
    public List<Map<String, Object>> list(Authentication auth) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        List<Chapter> chapters = chapterRepo.findAllByOrderByOrderIndexAsc();
        Map<String, ChapterProgress> progressMap = progressRepo.findByUserId(user.getId())
                .stream()
                .collect(Collectors.toMap(ChapterProgress::getChapterCode, p -> p));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Chapter ch : chapters) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", ch.getCode());
            item.put("cefrLevel", ch.getCefrLevel());
            item.put("titleEn", ch.getTitleEn());
            item.put("titleZh", ch.getTitleZh());
            item.put("orderIndex", ch.getOrderIndex());
            item.put("days", ch.getDays());

            ChapterProgress cp = progressMap.get(ch.getCode());
            if (cp != null) {
                item.put("phase", cp.getPhase());
                item.put("bossDefeated", cp.isBossDefeated());
            } else {
                item.put("phase", "locked");
                item.put("bossDefeated", false);
            }
            result.add(item);
        }
        return result;
    }
}
