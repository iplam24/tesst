package com.schedule.schedule.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;
    private boolean success; // Thêm trường này để Frontend check nhanh
}