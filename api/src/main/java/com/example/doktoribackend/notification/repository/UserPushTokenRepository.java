package com.example.doktoribackend.notification.repository;

import com.example.doktoribackend.notification.domain.UserPushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserPushTokenRepository extends JpaRepository<UserPushToken, Long> {

    Optional<UserPushToken> findByToken(String token);

    @Query("SELECT upt FROM UserPushToken upt " +
            "JOIN UserPreference up ON up.user.id = upt.userId " +
            "WHERE upt.userId IN :userIds AND up.notificationAgreement = true")
    List<UserPushToken> findByUserIdsWithNotificationEnabled(@Param("userIds") List<Long> userIds);
}