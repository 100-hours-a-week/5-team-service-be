package com.example.doktoribackend.config;

import com.example.doktoribackend.security.CustomAccessDeniedHandler;
import com.example.doktoribackend.security.CustomAuthenticationEntryPoint;
import com.example.doktoribackend.security.jwt.JwtAuthenticationFilter;
import com.example.doktoribackend.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import static com.example.doktoribackend.security.SecurityPaths.PUBLIC_AUTH;
import static com.example.doktoribackend.security.SecurityPaths.PUBLIC_DOCS;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                // 헤더로 아이디/비번 보내는 방식 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_AUTH).permitAll()
                        .requestMatchers(PUBLIC_DOCS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/meetings").permitAll()
                        .requestMatchers(HttpMethod.GET, "/meetings/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/meetings/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/recommendations/meetings").permitAll()
                        .anyRequest().authenticated()
                )
                // JWT 인증 필터 추가
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
