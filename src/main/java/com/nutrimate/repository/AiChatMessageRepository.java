package com.nutrimate.repository;

import com.nutrimate.entity.AiChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, String> {

    List<AiChatMessage> findByUser_IdOrderByCreatedAtAsc(String userId);

    List<AiChatMessage> findByUser_IdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
