package com.lingobuddy.controller;

import com.lingobuddy.dto.CheckinDto;
import com.lingobuddy.entity.DailyCheckin;
import com.lingobuddy.entity.User;
import com.lingobuddy.repository.UserRepository;
import com.lingobuddy.service.CheckinService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/checkin")
public class CheckinController {

    private final CheckinService checkinService;
    private final UserRepository userRepository;

    public CheckinController(CheckinService checkinService, UserRepository userRepository) {
        this.checkinService = checkinService;
        this.userRepository = userRepository;
    }

    @PostMapping("/today")
    public ResponseEntity<?> checkin() {
        User user = getCurrentUser();
        DailyCheckin result = checkinService.performCheckin(user);
        return ResponseEntity.ok(Map.of(
            "date", result.getCheckinDate().toString(),
            "xpEarned", result.getXpEarned(),
            "coinsEarned", result.getCoinsEarned(),
            "streak", result.getStreak()
        ));
    }

    @GetMapping("/calendar")
    public ResponseEntity<?> calendar(@RequestParam int year, @RequestParam int month) {
        User user = getCurrentUser();
        List<DailyCheckin> records = checkinService.getCalendar(user.getId(), year, month);
        List<CheckinDto> dtos = records.stream().map(c -> {
            CheckinDto dto = new CheckinDto();
            dto.setDate(c.getCheckinDate());
            dto.setXpEarned(c.getXpEarned());
            dto.setCoinsEarned(c.getCoinsEarned());
            dto.setStreak(c.getStreak());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }
}
