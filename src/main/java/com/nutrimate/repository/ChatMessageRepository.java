package com.nutrimate.repository;

import com.nutrimate.dto.RecentChatResponse;
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
            @Param("user1") String user1Id,
            @Param("user2") String user2Id,
            @Param("chatType") ChatType chatType);

    @Query("SELECT new com.nutrimate.dto.RecentChatResponse(" +
            "CASE WHEN m.senderId = :userId THEN m.receiverId ELSE m.senderId END, " +
            "CASE WHEN m.senderId = :userId THEN COALESCE(receiver.fullName, '') ELSE COALESCE(sender.fullName, '') END, " +
            "CASE WHEN m.senderId = :userId THEN receiver.avatarUrl ELSE sender.avatarUrl END, " +
            "m.content, " +
            "m.timestamp, " +
            "m.chatType) " +
            "FROM ChatMessage m " +
            "JOIN User sender ON sender.id = m.senderId " +
            "JOIN User receiver ON receiver.id = m.receiverId " +
            "WHERE (m.senderId = :userId OR m.receiverId = :userId) " +
            "AND m.timestamp = (" +
            "   SELECT MAX(m2.timestamp) FROM ChatMessage m2 " +
            "   WHERE m2.chatType = m.chatType " +
            "     AND ((m2.senderId = m.senderId AND m2.receiverId = m.receiverId) " +
            "       OR (m2.senderId = m.receiverId AND m2.receiverId = m.senderId))" +
            ") " +
            "ORDER BY m.timestamp DESC")
    List<RecentChatResponse> findRecentChatsForUser(@Param("userId") String userId);
}
