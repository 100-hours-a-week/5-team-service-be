package com.example.doktoribackend.auth.repository;

import com.example.doktoribackend.auth.domain.RefreshTokenRedis;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RefreshTokenRedisRepository extends CrudRepository<RefreshTokenRedis, String> {

    /**
     * 특정 사용자의 모든 RefreshToken 조회
     */
    List<RefreshTokenRedis> findAllByUserId(Long userId);

    /**
     * 특정 사용자의 모든 RefreshToken 삭제 (회원 탈퇴 시 사용)
     */
    void deleteAllByUserId(Long userId);
}