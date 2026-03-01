package com.example.doktoribackend.common.swagger;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = """
                                {
                                  "code": "AUTH_UNAUTHORIZED",
                                  "message": "인증이 필요합니다."
                                }
                                """))),
        @ApiResponse(responseCode = "403", description = "Forbidden",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(value = """
                                {
                                  "code": "FORBIDDEN",
                                  "message": "접근 권한이 없습니다."
                                }
                                """)))
})
public @interface AuthErrorResponses {
}
