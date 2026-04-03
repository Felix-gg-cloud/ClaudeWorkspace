package com.ll.ai.repository;

import com.ll.ai.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    List<ChatMessage> findTop30BySessionIdOrderByCreatedAtDesc(Long sessionId);

    long countBySessionId(Long sessionId);
}
