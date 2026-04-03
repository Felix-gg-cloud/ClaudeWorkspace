package com.ll.user.service;

import com.ll.common.exception.BizException;
import com.ll.common.util.JwtUtil;
import com.ll.user.entity.User;
import com.ll.user.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public Map<String, Object> register(String username, String password, String displayName, String grade) {
        if (username == null || username.trim().length() < 2) {
            throw new BizException("用户名至少2个字符");
        }
        if (password == null || password.length() < 4) {
            throw new BizException("密码至少4个字符");
        }
        if (userRepo.existsByUsername(username.trim())) {
            throw new BizException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPasswordHash(encoder.encode(password));
        user.setDisplayName(displayName != null && !displayName.isBlank() ? displayName.trim() : username.trim());
        user.setGrade(grade != null ? grade : "junior");
        userRepo.save(user);

        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        return Map.of("token", token, "userId", user.getId(), "username", user.getUsername());
    }

    public Map<String, Object> login(String username, String password) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new BizException("用户名或密码错误"));

        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new BizException("用户名或密码错误");
        }

        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        return Map.of("token", token, "userId", user.getId(), "username", user.getUsername());
    }

    public User getUserById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
    }

    public User updateUser(Long id, String displayName, String grade) {
        User user = getUserById(id);
        if (displayName != null) user.setDisplayName(displayName);
        if (grade != null) user.setGrade(grade);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepo.save(user);
    }
}
