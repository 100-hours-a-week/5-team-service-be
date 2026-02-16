package com.example.doktoribackend.room.repository;

import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.RoomStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChattingRoomRepository extends JpaRepository<ChattingRoom, Long> {

    @Query("SELECT r FROM ChattingRoom r LEFT JOIN FETCH r.book WHERE r.status = :status AND (:cursorId IS NULL OR r.id < :cursorId) ORDER BY r.id DESC")
    List<ChattingRoom> findByStatusWithCursor(@Param("status") RoomStatus status, @Param("cursorId") Long cursorId, Pageable pageable);
}
