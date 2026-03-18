package com.example.doktoribackend.review.domain;

import com.example.doktoribackend.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "image_path", nullable = false, length = 512)
    private String imagePath;

    @Column(name = "image_order", nullable = false)
    private Integer imageOrder;

    public static ReviewImage create(String imagePath, Integer imageOrder) {
        ReviewImage image = new ReviewImage();
        image.imagePath = imagePath;
        image.imageOrder = imageOrder;
        return image;
    }

    void assignReview(Review review) {
        this.review = review;
    }
}
