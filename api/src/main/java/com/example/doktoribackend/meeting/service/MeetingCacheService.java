package com.example.doktoribackend.meeting.service;

import com.example.doktoribackend.meeting.dto.MeetingDetailResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class MeetingCacheService {

    private static final String KEY_PREFIX = "meeting:detail:";
    private static final String NULL_MARKER = "NULL";

    private static final Duration BASE_TTL = Duration.ofMinutes(30);
    private static final int JITTER_SECONDS = 300;
    private static final Duration NULL_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public MeetingCacheService(
            StringRedisTemplate redisTemplate,
            @Qualifier("cacheObjectMapper") ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<MeetingDetailResponse> getDetail(Long meetingId) {
        String key = KEY_PREFIX + meetingId;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            log.debug("캐시 MISS: meetingId={}", meetingId);
            return null;
        }

        if (NULL_MARKER.equals(value)) {
            log.debug("캐시 NULL MARKER HIT: meetingId={}", meetingId);
            return Optional.empty();
        }

        try {
            MeetingDetailResponse response = objectMapper.readValue(value, MeetingDetailResponse.class);
            log.debug("캐시 HIT: meetingId={}", meetingId);
            return Optional.of(response);
        } catch (JsonProcessingException e) {
            log.warn("캐시 역직렬화 실패, 캐시 삭제: meetingId={}", meetingId, e);
            redisTemplate.delete(key);
            return null;
        }
    }

    public void putDetail(Long meetingId, MeetingDetailResponse response) {
        String key = KEY_PREFIX + meetingId;
        try {
            String json = objectMapper.writeValueAsString(response);
            Duration ttl = BASE_TTL.plusSeconds(ThreadLocalRandom.current().nextInt(JITTER_SECONDS));
            redisTemplate.opsForValue().set(key, json, ttl);
            log.debug("캐시 저장: meetingId={}, ttl={}s", meetingId, ttl.getSeconds());
        } catch (JsonProcessingException e) {
            log.warn("캐시 직렬화 실패: meetingId={}", meetingId, e);
        }
    }

    public void putNullMarker(Long meetingId) {
        String key = KEY_PREFIX + meetingId;
        redisTemplate.opsForValue().set(key, NULL_MARKER, NULL_TTL);
        log.debug("캐시 NULL MARKER 저장: meetingId={}", meetingId);
    }

    public void evictDetail(Long meetingId) {
        String key = KEY_PREFIX + meetingId;
        redisTemplate.delete(key);
        log.debug("캐시 삭제: meetingId={}", meetingId);
    }
}
