package com.example.doktoribackend.review.repository;

import com.example.doktoribackend.review.domain.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByMeetingRoundIdAndReviewerIdAndDeletedAtIsNull(Long meetingRoundId, Long reviewerId);


    @Query("SELECT r.id FROM Review r " +
            "JOIN r.meetingRound mr " +
            "JOIN mr.meeting m " +
            "WHERE m.leaderUser.id = :leaderUserId " +
            "AND r.deletedAt IS NULL " +
            "AND (:cursorId IS NULL OR r.id < :cursorId) " +
            "ORDER BY r.id DESC")
    List<Long> findIdsByLeaderUserIdWithCursor(@Param("leaderUserId") Long leaderUserId,
                                               @Param("cursorId") Long cursorId,
                                               Pageable pageable);

    @Query("SELECT r.id FROM Review r " +
            "WHERE r.reviewer.id = :reviewerId " +
            "AND r.deletedAt IS NULL " +
            "AND (:cursorId IS NULL OR r.id < :cursorId) " +
            "ORDER BY r.id DESC")
    List<Long> findIdsByReviewerIdWithCursor(@Param("reviewerId") Long reviewerId,
                                             @Param("cursorId") Long cursorId,
                                             Pageable pageable);

    @Query("SELECT r FROM Review r " +
            "JOIN FETCH r.reviewer " +
            "LEFT JOIN FETCH r.images " +
            "WHERE r.id IN :ids " +
            "ORDER BY r.id DESC")
    List<Review> findAllWithReviewerAndImagesByIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT r FROM Review r " +
            "LEFT JOIN FETCH r.images " +
            "WHERE r.id IN :ids " +
            "ORDER BY r.id DESC")
    List<Review> findAllWithImagesByIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT r FROM Review r " +
            "JOIN FETCH r.reviewer " +
            "JOIN FETCH r.meetingRound mr " +
            "JOIN FETCH mr.meeting " +
            "LEFT JOIN FETCH r.images " +
            "WHERE r.id = :reviewId AND r.deletedAt IS NULL")
    Optional<Review> findByIdWithReviewerAndRoundAndImages(@Param("reviewId") Long reviewId);

    Optional<Review> findByReviewerIdAndMeetingRoundIdAndDeletedAtIsNull(Long reviewerId, Long meetingRoundId);

    long countByMeetingRoundIdAndDeletedAtIsNull(Long meetingRoundId);

    @Query("SELECT r.bestMemberId FROM Review r " +
            "WHERE r.meetingRound.id = :meetingRoundId " +
            "AND r.deletedAt IS NULL " +
            "AND r.bestMemberId IS NOT NULL " +
            "GROUP BY r.bestMemberId " +
            "HAVING COUNT(r.bestMemberId) = (" +
            "  SELECT MAX(cnt) FROM (" +
            "    SELECT COUNT(r2.bestMemberId) AS cnt FROM Review r2 " +
            "    WHERE r2.meetingRound.id = :meetingRoundId " +
            "    AND r2.deletedAt IS NULL " +
            "    AND r2.bestMemberId IS NOT NULL " +
            "    GROUP BY r2.bestMemberId" +
            "  ) sub" +
            ")")
    List<Long> findBestMemberIdsByMeetingRoundId(@Param("meetingRoundId") Long meetingRoundId);
}
