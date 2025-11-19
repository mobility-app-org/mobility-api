package com.mobility.api.global.annotation;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <pre>
 *     Pageable 객체를 swagger에 정리하기 위한 어노테이션
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Parameters({
        @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", in = ParameterIn.QUERY,
                schema = @Schema(type = "integer", defaultValue = "0")),
        @Parameter(name = "size", description = "페이지 크기", in = ParameterIn.QUERY,
                schema = @Schema(type = "integer", defaultValue = "10")),
        @Parameter(name = "sort", description = "정렬 기준 (예: createdAt,desc)", in = ParameterIn.QUERY,
                schema = @Schema(type = "string"))
})
public @interface SwaggerPageable {
}