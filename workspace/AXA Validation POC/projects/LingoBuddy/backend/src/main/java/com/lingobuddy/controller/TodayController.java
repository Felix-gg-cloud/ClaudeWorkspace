package com.lingobuddy.controller;

import com.lingobuddy.dto.TaskCompleteResult;
import com.lingobuddy.dto.TodayLessonDto;
import com.lingobuddy.entity.User;
import com.lingobuddy.repository.UserRepository;
import com.lingobuddy.service.LessonService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TodayController {

    private final LessonService lessonService;
    private final UserRepository userRepository;

    public TodayController(LessonService lessonService, UserRepository userRepository) {
        this.lessonService = lessonService;
        this.userRepository = userRepository;
    }

    @GetMapping("/today")
    public ResponseEntity<?> getTodayLesson() {
        User user = getCurrentUser();
        TodayLessonDto lesson = lessonService.getTodayLesson(user);
        if (lesson == null) {
            return ResponseEntity.ok(Map.of("message", "暂无课程安排"));
        }
        return ResponseEntity.ok(lesson);
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<?> completeTask(@PathVariable Long taskId) {
        User user = getCurrentUser();
        TaskCompleteResult result = lessonService.completeTask(user, taskId);
        return ResponseEntity.ok(result);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }
}
