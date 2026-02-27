package com.example.doktoribackend.vote.service;

import com.example.doktoribackend.common.error.ErrorCode;
import com.example.doktoribackend.exception.BusinessException;
import com.example.doktoribackend.room.domain.ChattingRoom;
import com.example.doktoribackend.room.domain.Position;
import com.example.doktoribackend.room.repository.ChattingRoomMemberRepository;
import com.example.doktoribackend.vote.domain.Vote;
import com.example.doktoribackend.vote.domain.VoteCast;
import com.example.doktoribackend.vote.domain.VoteCastId;
import com.example.doktoribackend.vote.dto.VoteResultResponse;
import com.example.doktoribackend.vote.repository.VoteCastRepository;
import com.example.doktoribackend.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoteService {

    private static final Duration VOTE_DURATION = Duration.ofMinutes(1);

    private final VoteRepository voteRepository;
    private final VoteCastRepository voteCastRepository;
    private final ChattingRoomMemberRepository chattingRoomMemberRepository;

    @Transactional
    public void createVote(ChattingRoom room, int totalMemberCount) {
        Vote vote = Vote.create(room, totalMemberCount);
        voteRepository.save(vote);
    }

    @Transactional
    public void castVote(Long roomId, Long userId, Position choice) {
        Vote vote = findVote(roomId);
        closeIfExpired(vote);

        if (vote.isClosed()) {
            throw new BusinessException(ErrorCode.VOTE_ALREADY_CLOSED);
        }

        validateMember(roomId, userId);

        if (voteCastRepository.existsById(new VoteCastId(roomId, userId))) {
            throw new BusinessException(ErrorCode.VOTE_ALREADY_CAST);
        }

        VoteCast voteCast = VoteCast.create(vote, userId, choice);
        voteCastRepository.save(voteCast);
        vote.incrementCount(choice);
    }

    @Transactional(readOnly = true)
    public VoteResultResponse getVoteResult(Long roomId, Long userId) {
        Vote vote = findVote(roomId);
        closeIfExpired(vote);

        validateMember(roomId, userId);

        Position myChoice = voteCastRepository.findById(new VoteCastId(roomId, userId))
                .map(VoteCast::getChoice)
                .orElse(null);

        return new VoteResultResponse(
                vote.getAgreeCount(),
                vote.getDisagreeCount(),
                vote.getTotalMemberCount(),
                vote.isClosed(),
                myChoice
        );
    }

    private Vote findVote(Long roomId) {
        return voteRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));
    }

    private void closeIfExpired(Vote vote) {
        if (!vote.isClosed() && vote.getOpenedAt().plus(VOTE_DURATION).isBefore(LocalDateTime.now())) {
            vote.close();
        }
    }

    private void validateMember(Long roomId, Long userId) {
        chattingRoomMemberRepository.findByChattingRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND));
    }
}
