package com.example.doktoribackend.zoom.service;

import com.example.doktoribackend.config.ZoomConfig;
import com.example.doktoribackend.zoom.exception.ZoomApiException;
import com.example.doktoribackend.zoom.exception.ZoomAuthenticationException;
import com.example.doktoribackend.zoom.exception.ZoomRetryableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Zoom 미팅 생성.
 *
 * <p>재시도는 걸지 않는다. {@link ZoomLinkSchedulerService} 가 1분마다 10분치를 미리 처리하므로
 * 이미 사실상 10회의 자연 재시도가 존재한다. 여기에 in-call 재시도를 얹으면 10분간 최대 30회
 * 호출이 되어 Zoom rate limit(429)을 오히려 유발한다.
 */
@Slf4j
@Service
public class ZoomService {

    private static final int DEFAULT_DURATION_MINUTES = 60;

    private final ZoomConfig zoomConfig;
    private final RestClient restClient;
    private final ZoomAccessTokenProvider accessTokenProvider;

    public ZoomService(RestClient zoomRestClient, ZoomConfig zoomConfig,
                       ZoomAccessTokenProvider accessTokenProvider) {
        this.restClient = zoomRestClient;
        this.zoomConfig = zoomConfig;
        this.accessTokenProvider = accessTokenProvider;
    }

    @CircuitBreaker(name = "zoom")
    public String createMeeting(String topic, LocalDateTime startTime, int durationMinutes) {
        try {
            String accessToken = accessTokenProvider.getAccessToken();
            String meetingUrl = zoomConfig.getApiBaseUrl() + "/users/me/meetings";
            Map<String, Object> meetingRequest = buildMeetingRequest(topic, startTime, durationMinutes);

            Map<String, Object> responseBody = restClient.post()
                    .uri(meetingUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(meetingRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        int statusCode = res.getStatusCode().value();
                        String body = new String(res.getBody().readAllBytes());
                        if (statusCode == 401) {
                            log.error("Zoom API 인증 실패 - Response: {}", body);
                            throw new ZoomAuthenticationException("Zoom API 인증에 실패했습니다.");
                        }
                        if (statusCode == 429) {
                            log.warn("Zoom API Rate Limit 초과 - Response: {}", body);
                            throw new ZoomRetryableException("Zoom API Rate Limit 초과 (재시도 가능)");
                        }
                        log.error("Zoom API 클라이언트 오류 - Status: {}, Body: {}", statusCode, body);
                        throw new ZoomApiException("Zoom API 클라이언트 오류");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("Zoom API 서버 오류 - Status: {}, Body: {}",
                                res.getStatusCode(), new String(res.getBody().readAllBytes()));
                        throw new ZoomRetryableException("Zoom API 서버 오류 (재시도 가능)");
                    })
                    .body(new ParameterizedTypeReference<>() {});

            if (responseBody == null) {
                log.error("Zoom API 응답 body가 null - Topic: {}, StartTime: {}", topic, startTime);
                throw new ZoomApiException("Zoom API 응답이 비어있습니다.");
            }

            return (String) responseBody.get("join_url");

        } catch (ResourceAccessException e) {
            log.error("Zoom API 네트워크 오류", e);
            throw new ZoomRetryableException("Zoom API 네트워크 오류 (재시도 가능)", e);
        } catch (ZoomAuthenticationException | ZoomRetryableException | ZoomApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Zoom 미팅 생성 실패", e);
            throw new ZoomApiException("Zoom 미팅 생성에 실패했습니다.", e);
        }
    }

    public String createMeeting(String topic, LocalDateTime startTime) {
        return createMeeting(topic, startTime, DEFAULT_DURATION_MINUTES);
    }

    private Map<String, Object> buildMeetingRequest(String topic, LocalDateTime startTime, int durationMinutes) {
        Map<String, Object> meetingRequest = new HashMap<>();
        meetingRequest.put("topic", topic);
        meetingRequest.put("type", 2);
        meetingRequest.put("start_time", formatStartTime(startTime));
        meetingRequest.put("duration", durationMinutes);
        meetingRequest.put("timezone", "Asia/Seoul");

        Map<String, Object> settings = new HashMap<>();
        settings.put("host_video", true);
        settings.put("participant_video", true);
        settings.put("join_before_host", true);
        settings.put("mute_upon_entry", true);
        settings.put("waiting_room", false);
        settings.put("auto_recording", "none");
        meetingRequest.put("settings", settings);

        return meetingRequest;
    }

    private String formatStartTime(LocalDateTime startTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return startTime.format(formatter);
    }
}
