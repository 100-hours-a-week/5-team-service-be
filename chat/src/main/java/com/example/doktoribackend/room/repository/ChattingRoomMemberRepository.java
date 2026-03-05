package com.example.doktoribackend.room.repository;

import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.domain.MemberStatus;
import com.example.doktoribackend.room.domain.Position;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChattingRoomMemberRepository extends JpaRepository<ChattingRoomMember, Long> {

    boolean existsByUserIdAndStatusIn(Long userId, List<MemberStatus> statuses);

    Optional<ChattingRoomMember> findFirstByUserIdAndStatusIn(Long userId, List<MemberStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM ChattingRoomMember m WHERE m.userId = :userId AND m.status IN :statuses")
    List<ChattingRoomMember> findByUserIdAndStatusInWithLock(@Param("userId") Long userId, @Param("statuses") List<MemberStatus> statuses);

    Optional<ChattingRoomMember> findByChattingRoomIdAndUserId(Long roomId, Long userId);

    List<ChattingRoomMember> findByChattingRoomIdAndStatusIn(Long roomId, List<MemberStatus> statuses);

    List<ChattingRoomMember> findByChattingRoomIdAndUserIdIn(Long chattingRoomId, Collection<Long> userIds);

    int countByChattingRoomIdAndPositionAndStatusIn(Long roomId, Position position, List<MemberStatus> statuses);
}
