package com.example.doktoribackend.room.repository;

import com.example.doktoribackend.room.domain.ChattingRoomMember;
import com.example.doktoribackend.room.domain.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChattingRoomMemberRepository extends JpaRepository<ChattingRoomMember, Long> {

    boolean existsByUserIdAndStatusIn(Long userId, List<MemberStatus> statuses);

    Optional<ChattingRoomMember> findByChattingRoomIdAndUserId(Long roomId, Long userId);

    List<ChattingRoomMember> findByChattingRoomIdAndStatusIn(Long roomId, List<MemberStatus> statuses);
}
