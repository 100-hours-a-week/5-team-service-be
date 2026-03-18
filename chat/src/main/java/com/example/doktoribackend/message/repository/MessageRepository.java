package com.example.doktoribackend.message.repository;

import com.example.doktoribackend.message.domain.Message;
import com.example.doktoribackend.message.domain.MessageType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends MongoRepository<Message, String> {

    Optional<Message> findByRoomIdAndSenderIdAndClientMessageId(Long roomId, Long senderId, String clientMessageId);

    List<Message> findByRoomIdOrderByIdDesc(Long roomId, Pageable pageable);

    List<Message> findByRoomIdAndIdLessThanOrderByIdDesc(Long roomId, String id, Pageable pageable);

    List<Message> findByRoundIdAndMessageTypeOrderByIdDesc(Long roundId, MessageType messageType);
}
