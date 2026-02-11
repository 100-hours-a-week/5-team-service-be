package com.example.doktoribackend.zoom.service;

import com.example.doktoribackend.config.ZoomConfig;
import com.example.doktoribackend.zoom.exception.ZoomApiException;
import com.example.doktoribackend.zoom.exception.ZoomAuthenticationException;
import com.example.doktoribackend.zoom.exception.ZoomRetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZoomService {

    private final ZoomConfig zoomConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final int DEFAULT_DURATION_MINUTES = 60;

    public String createMeeting(String topic, LocalDateTime startTime, int durationMinutes) {
        try {
            String accessToken = getAccessToken();

            String meetingUrl = zoomConfig.getApiBaseUrl() + "/users/me/meetings";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> meetingRequest = buildMeetingRequest(topic, startTime, durationMinutes);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(meetingRequest, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    meetingUrl,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null) {
                log.error("Zoom API 응답 body가 null - Topic: {}, StartTime: {}", topic, startTime);
                throw new ZoomApiException("Zoom API 응답이 비어있습니다.");
            }

            return (String) responseBody.get("join_url");

        } catch (HttpClientErrorException e) {
            handleClientError(e);
            throw new ZoomApiException("Zoom API 클라이언트 오류", e);
        } catch (HttpServerErrorException e) {
            log.error("Zoom API 서버 오류 - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ZoomRetryableException("Zoom API 서버 오류 (재시도 가능)", e);
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

    private void handleClientError(HttpClientErrorException e) {
        HttpStatusCode statusCode = e.getStatusCode();
        String responseBody = e.getResponseBodyAsString();

        if (statusCode.value() == 401) {
            log.error("Zoom API 인증 실패 - Response: {}", responseBody);
            throw new ZoomAuthenticationException("Zoom API 인증에 실패했습니다.", e);
        }

        if (statusCode.value() == 429) {
            log.warn("Zoom API Rate Limit 초과 - Response: {}", responseBody);
            throw new ZoomRetryableException("Zoom API Rate Limit 초과 (재시도 가능)", e);
        }

        log.error("Zoom API 클라이언트 오류 - Status: {}, Body: {}", statusCode, responseBody);
    }

    private String getAccessToken() {
        try {
            String tokenUrl = "https://zoom.us/oauth/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String auth = zoomConfig.getClientId() + ":" + zoomConfig.getClientSecret();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "account_credentials");
            body.add("account_id", zoomConfig.getAccountId());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> responseBody = response.getBody();
            return (String) Objects.requireNonNull(responseBody).get("access_token");

        } catch (HttpClientErrorException e) {
            log.error("Zoom Access Token 발급 실패 - Status: {}, Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new ZoomAuthenticationException("Zoom 인증에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("Zoom Access Token 발급 실패", e);
            throw new ZoomAuthenticationException("Zoom 인증에 실패했습니다.", e);
        }
    }

    private String formatStartTime(LocalDateTime startTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return startTime.format(formatter);
    }
}
