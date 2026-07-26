package com.example.doktoribackend.config;

import com.example.doktoribackend.common.client.book.AladinBookSearchClient;
import com.example.doktoribackend.common.client.book.BookSearchClient;
import com.example.doktoribackend.common.client.book.BookSearchGateway;
import com.example.doktoribackend.common.client.book.KakaoBookSearchClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 도서 검색 공급자 배선.
 *
 * <p>타임아웃을 반드시 지정한다. RestClient 기본값은 무한 대기라 외부 API 가 응답하지 않으면
 * 톰캣 스레드가 그대로 묶인다. Resilience4j 의 TimeLimiter 는 CompletableFuture 기반이라
 * 동기 RestClient 에는 적용되지 않으므로 타임아웃은 이 레이어에서만 보장할 수 있다.
 */
@Configuration
public class BookSearchClientConfig {

    @Bean
    public BookSearchClient kakaoBookSearchClient(
            @Value("${kakao.book.base-url}") String baseUrl,
            @Value("${kakao.book.rest-api-key}") String restApiKey,
            @Value("${kakao.book.connect-timeout:2s}") Duration connectTimeout,
            @Value("${kakao.book.read-timeout:3s}") Duration readTimeout
    ) {
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "KakaoAK " + restApiKey)
                .requestFactory(requestFactory(connectTimeout, readTimeout))
                .build();
        return new KakaoBookSearchClient(restClient);
    }

    @Bean
    public BookSearchClient aladinBookSearchClient(
            @Value("${aladin.book.base-url}") String baseUrl,
            @Value("${aladin.book.ttb-key}") String ttbKey,
            @Value("${aladin.book.connect-timeout:2s}") Duration connectTimeout,
            @Value("${aladin.book.read-timeout:3s}") Duration readTimeout
    ) {
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory(connectTimeout, readTimeout))
                .build();
        return new AladinBookSearchClient(restClient, ttbKey);
    }

    @Bean
    public BookSearchGateway bookSearchGateway(
            @Qualifier("kakaoBookSearchClient") BookSearchClient primary,
            @Qualifier("aladinBookSearchClient") BookSearchClient secondary
    ) {
        return new BookSearchGateway(primary, secondary);
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
