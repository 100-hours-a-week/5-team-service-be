package com.example.doktoribackend.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateRendererTest {

    TemplateRenderer templateRenderer;

    @BeforeEach
    void setUp() {
        templateRenderer = new TemplateRenderer();
    }

    @Test
    @DisplayName("render: 단일 파라미터를 치환한다")
    void render_singleParameter() {
        // given
        String template = "/users/me/meetings/{meetingId}";
        Map<String, String> parameters = Map.of("meetingId", "123");

        // when
        String result = templateRenderer.render(template, parameters);

        // then
        assertThat(result).isEqualTo("/users/me/meetings/123");
    }

    @Test
    @DisplayName("render: 여러 파라미터를 치환한다")
    void render_multipleParameters() {
        // given
        String template = "모임 {meetingTitle}의 {roundNo}회차가 시작됩니다";
        Map<String, String> parameters = Map.of(
                "meetingTitle", "독서 토론",
                "roundNo", "3"
        );

        // when
        String result = templateRenderer.render(template, parameters);

        // then
        assertThat(result).isEqualTo("모임 독서 토론의 3회차가 시작됩니다");
    }

    @Test
    @DisplayName("render: 파라미터가 없으면 원본 템플릿에서 placeholder를 유지한다")
    void render_missingParameter_keepsPlaceholder() {
        // given
        String template = "/users/me/meetings/{meetingId}";
        Map<String, String> parameters = Map.of();

        // when
        String result = templateRenderer.render(template, parameters);

        // then
        assertThat(result).isEqualTo("/users/me/meetings/{meetingId}");
    }

    @Test
    @DisplayName("render: null 템플릿은 null을 반환한다")
    void render_nullTemplate_returnsNull() {
        // when
        String result = templateRenderer.render(null, Map.of("key", "value"));

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("render: null 파라미터는 원본 템플릿을 반환한다")
    void render_nullParameters_returnsTemplate() {
        // given
        String template = "Hello {name}";

        // when
        String result = templateRenderer.render(template, null);

        // then
        assertThat(result).isEqualTo("Hello {name}");
    }

    @Test
    @DisplayName("render: 빈 파라미터는 원본 템플릿을 반환한다")
    void render_emptyParameters_returnsTemplate() {
        // given
        String template = "Hello {name}";

        // when
        String result = templateRenderer.render(template, Map.of());

        // then
        assertThat(result).isEqualTo("Hello {name}");
    }

    @Test
    @DisplayName("render: placeholder가 없는 템플릿은 그대로 반환한다")
    void render_noPlaceholder_returnsAsIs() {
        // given
        String template = "안녕하세요. 곧 토론이 시작됩니다.";

        // when
        String result = templateRenderer.render(template, Map.of("unused", "value"));

        // then
        assertThat(result).isEqualTo("안녕하세요. 곧 토론이 시작됩니다.");
    }

    @Test
    @DisplayName("render: 특수문자가 포함된 값도 정상 치환한다")
    void render_specialCharacters_handledCorrectly() {
        // given
        String template = "제목: {title}";
        Map<String, String> parameters = Map.of("title", "책 제목 (특별판) $100");

        // when
        String result = templateRenderer.render(template, parameters);

        // then
        assertThat(result).isEqualTo("제목: 책 제목 (특별판) $100");
    }
}
