package com.eqa.service;

import com.eqa.entity.ChapterProgress;
import com.eqa.entity.User;
import com.eqa.repository.ChapterProgressRepository;
import com.eqa.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ChapterProgressRepository chapterProgressRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       ChapterProgressRepository chapterProgressRepo,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.chapterProgressRepo = chapterProgressRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String username, String password, String displayName) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName != null ? displayName : username);
        user = userRepository.save(user);

        // 自动解锁第一章
        ChapterProgress cp = new ChapterProgress();
        cp.setUserId(user.getId());
        cp.setChapterCode("PRE_A1_CH1");
        cp.setPhase("quest");
        chapterProgressRepo.save(cp);

        return user;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
