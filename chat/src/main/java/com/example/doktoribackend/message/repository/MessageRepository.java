package com.example.doktoribackend.message.repository;

import com.example.doktoribackend.message.domain.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    boolean existsByRoomIdAndSenderIdAndClientMessageId(Long roomId, Long senderId, String clientMessageId);

    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId AND (:cursorId IS NULL OR m.id < :cursorId) ORDER BY m.id DESC")
    List<Message> findByRoomIdWithCursor(@Param("roomId") Long roomId, @Param("cursorId") Long cursorId, Pageable pageable);
}
