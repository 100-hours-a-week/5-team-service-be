package com.example.doktoribackend.room.repository;

import com.example.doktoribackend.room.domain.ChattingRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChattingRoomRepository extends JpaRepository<ChattingRoom, Long> {
}
