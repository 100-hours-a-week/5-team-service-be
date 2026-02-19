package com.example.doktoribackend.message.repository;

import com.example.doktoribackend.message.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    boolean existsByRoomIdAndSenderIdAndClientMessageId(Long roomId, Long senderId, String clientMessageId);
}
