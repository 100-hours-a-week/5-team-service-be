package com.example.doktoribackend.notification.repository;

import com.example.doktoribackend.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n " +
            "WHERE n.user.id = :userId " +
            "AND n.deletedAt IS NULL " +
            "AND n.createdAt >= :since " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findRecentByUserId(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END " +
            "FROM Notification n " +
            "WHERE n.user.id = :userId " +
            "AND n.isRead = false " +
            "AND n.deletedAt IS NULL " +
            "AND n.createdAt >= :since")
    boolean existsUnreadByUserIdSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );
}
