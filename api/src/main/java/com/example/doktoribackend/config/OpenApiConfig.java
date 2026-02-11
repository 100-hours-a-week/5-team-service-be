package com.example.doktoribackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
class OpenApiConfig {
    @Bean
    OpenAPI openApi() {
        String bearerSchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Doktori API")
                        .version("v1")
                        .description("Doktori API 문서"))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("Local Server"),
                        new Server().url("https://dev.doktori.kr/api").description("Development Server"),
                        new Server().url("https://doktori.kr/api").description("Production Server")
                ))
                .components(new Components()
                        .addSecuritySchemes(bearerSchemeName,
                                new SecurityScheme()
                                        .name(bearerSchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("Bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(bearerSchemeName));
    }
}
