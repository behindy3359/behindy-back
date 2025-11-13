package com.example.backend.repository.multiplayer;

import com.example.backend.entity.multiplayer.ChatMessage;
import com.example.backend.entity.multiplayer.MessageType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE m.room.roomId = :roomId ORDER BY m.createdAt DESC")
    List<ChatMessage> findByRoomIdOrderByCreatedAtDesc(@Param("roomId") Long roomId, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.room.roomId = :roomId AND m.messageType = :type ORDER BY m.createdAt DESC")
    List<ChatMessage> findByRoomIdAndTypeOrderByCreatedAtDesc(@Param("roomId") Long roomId, @Param("type") MessageType type, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.room.roomId = :roomId AND m.messageType IN :types ORDER BY m.createdAt ASC")
    List<ChatMessage> findRecentUserMessages(@Param("roomId") Long roomId, @Param("types") List<MessageType> types, Pageable pageable);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.room.roomId = :roomId")
    long countByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT m FROM ChatMessage m WHERE m.room.roomId = :roomId AND m.messageType = 'PHASE' ORDER BY m.createdAt DESC")
    List<ChatMessage> findLatestPhaseMessage(@Param("roomId") Long roomId, Pageable pageable);
}
