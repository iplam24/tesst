package com.schedule.schedule.exception;

import com.schedule.schedule.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        String message = e.getMessage();

        // Chuyển mã lỗi kỹ thuật sang tiếng Việt thân thiện
        if ("SAI_TAI_KHOAN".equals(message)) {
            message = "Mã sinh viên hoặc mật khẩu không chính xác!";
        } else if (message.contains("Timeout")) {
            message = "Hệ thống Đào tạo phản hồi chậm, vui lòng thử lại sau!";
        }

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(400)
                .success(false)
                .message(message)
                .build());
    }
}