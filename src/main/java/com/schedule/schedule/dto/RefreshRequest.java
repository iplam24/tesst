package com.schedule.schedule.dto;

import lombok.Data;

// RefreshRequest.java
    @Data
    public class RefreshRequest {
        private String mssv;
        private String selectedSemester; // Chỉ gửi tên kỳ ở Bước 2

}
