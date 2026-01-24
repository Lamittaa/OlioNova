package com.project.auth_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDto{
    private final Instant timestamp;
    private final int status;
    private final String error;     // اسم الحالة (Bad Request, Not Found…)
    private final String message;   // الرسالة البشرية
    private final String path;      // المسار الذي حصل فيه الخطأ
    private final String code;      // كود داخلي اختياري (مثلاً REFRESH_TOKEN_EXPIRED)
    private final List<FieldErrorDto> errors; // أخطاء الحقول عند التحقق @Valid
}
