package com.example.doktoribackend.message.repository;

import com.example.doktoribackend.message.domain.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Optional<Message> findByRoomIdAndSenderIdAndClientMessageId(Long roomId, Long senderId, String clientMessageId);

    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId AND (:cursorId IS NULL OR m.id < :cursorId) ORDER BY m.id DESC")
    List<Message> findByRoomIdWithCursor(@Param("roomId") Long roomId, @Param("cursorId") Long cursorId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.roundId = :roundId AND m.messageType = 'TEXT' ORDER BY m.id DESC")
    List<Message> findTextMessagesByRoundIdDesc(@Param("roundId") Long roundId);
}
