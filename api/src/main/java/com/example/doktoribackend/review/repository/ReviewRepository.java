package com.example.doktoribackend.review.repository;

import com.example.doktoribackend.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByMeetingRoundIdAndReviewerIdAndDeletedAtIsNull(Long meetingRoundId, Long reviewerId);

    boolean existsByMeetingRoundIdAndReviewerIdAndDeletedAtIsNull(Long meetingRoundId, Long reviewerId);

    List<Review> findByReviewerIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long reviewerId);
}
