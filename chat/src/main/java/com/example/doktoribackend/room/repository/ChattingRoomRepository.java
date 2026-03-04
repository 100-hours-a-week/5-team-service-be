package com.example.doktoribackend.room.repository;

import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.RoomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChattingRoomRepository extends JpaRepository<ChattingRoom, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ChattingRoom r WHERE r.id = :id")
    Optional<ChattingRoom> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT r FROM ChattingRoom r LEFT JOIN FETCH r.book WHERE r.status = :status AND (:cursorId IS NULL OR r.id < :cursorId) ORDER BY r.id DESC")
    List<ChattingRoom> findByStatusWithCursor(@Param("status") RoomStatus status, @Param("cursorId") Long cursorId, Pageable pageable);


    @Query(value = "SELECT DISTINCT cr.* FROM chatting_rooms cr " +
            "JOIN room_rounds rr ON rr.room_id = cr.id AND rr.round_number = 1 " +
            "WHERE cr.status = 'CHATTING' " +
            "AND TIMESTAMPADD(MINUTE, cr.duration, rr.started_at) < :now",
            nativeQuery = true)
    List<ChattingRoom> findExpiredChattingRooms(@Param("now") LocalDateTime now);
}
