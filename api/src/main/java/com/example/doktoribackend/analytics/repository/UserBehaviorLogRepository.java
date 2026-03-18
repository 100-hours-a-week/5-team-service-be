package com.example.doktoribackend.analytics.repository;

import com.example.doktoribackend.analytics.domain.UserBehaviorLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBehaviorLogRepository extends MongoRepository<UserBehaviorLog, String> {
}
