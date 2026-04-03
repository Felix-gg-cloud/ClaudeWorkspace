package com.ll.ai.repository;

import com.ll.ai.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<ChatSession> findByUserIdAndTypeOrderByUpdatedAtDesc(Long userId, String type);

    Optional<ChatSession> findFirstByUserIdAndTypeAndStatusOrderByCreatedAtDesc(
            Long userId, String type, String status);
}
