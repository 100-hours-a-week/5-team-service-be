package com.example.doktoribackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {
    @Bean
    OpenAPI openApi() {
        String bearerSchemeName = "bearerAuth";

        Server devServer = new Server();
        devServer.setUrl("https://dev.doktori.kr");
        devServer.setDescription("Development Server");

        return new OpenAPI()
                .info(new Info()
                        .title("Doktori API")
                        .version("v1")
                        .description("Doktori API 문서"))
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
