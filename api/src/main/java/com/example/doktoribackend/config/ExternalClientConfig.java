package com.example.doktoribackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

/**
 * 외부 API 호출용 RestClient 배선.
 *
 * <p>RestClient 기본값은 connect/read 타임아웃이 없어 상대가 응답하지 않으면 톰캣 스레드가
 * 무기한 묶인다. Resilience4j 로는 막을 수 없는 구간이라 여기서 반드시 지정한다.
 */
@Configuration
public class ExternalClientConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    /**
     * 카카오 OAuth. 사용자가 로그인 화면에서 대기하는 동기 흐름이라 짧게 잡는다.
     */
    @Bean
    public RestClient kakaoOAuthRestClient(
            @Value("${kakao.oauth.connect-timeout:2s}") Duration connectTimeout,
            @Value("${kakao.oauth.read-timeout:3s}") Duration readTimeout
    ) {
        return RestClient.builder()
                .requestFactory(requestFactory(connectTimeout, readTimeout))
                .build();
    }

    @Bean
    public RestClient zoomRestClient(
            @Value("${zoom.connect-timeout:2s}") Duration connectTimeout,
            @Value("${zoom.read-timeout:5s}") Duration readTimeout
    ) {
        return RestClient.builder()
                .requestFactory(requestFactory(connectTimeout, readTimeout))
                .build();
    }

    private ClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(connectTimeout)
                        .build()
        );
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
