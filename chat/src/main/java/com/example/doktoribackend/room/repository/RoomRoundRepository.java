package com.example.doktoribackend.room.repository;

import com.example.doktoribackend.room.domain.RoomRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRoundRepository extends JpaRepository<RoomRound, Long> {

    Optional<RoomRound> findByChattingRoomIdAndEndedAtIsNull(Long roomId);

    List<RoomRound> findByChattingRoomIdOrderByRoundNumberAsc(Long roomId);
}
