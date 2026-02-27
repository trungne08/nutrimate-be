package com.nutrimate.repository;

import com.nutrimate.entity.ChatMessage;
import com.nutrimate.entity.ChatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE " +
            "m.chatType = :chatType AND (" +
            "(m.senderId = :user1 AND m.receiverId = :user2) OR " +
            "(m.senderId = :user2 AND m.receiverId = :user1)) " +
            "ORDER BY m.timestamp ASC")
    List<ChatMessage> findChatHistoryBetween(
            @Param("user1") Long user1,
            @Param("user2") Long user2,
            @Param("chatType") ChatType chatType);
}
