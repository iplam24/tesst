package com.schedule.schedule.controller;

import com.schedule.schedule.dto.ApiResponse;
import com.schedule.schedule.dto.LoginRequest;
import com.schedule.schedule.dto.RefreshRequest;
import com.schedule.schedule.dto.RefreshResponse;
import com.schedule.schedule.entity.TimeTableDaily;
import com.schedule.schedule.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tkb")
@RequiredArgsConstructor
public class TimeTableController {

    private final TimetableService timetableService;

    /**
     * API 1: Lấy danh sách học kỳ và lưu mật khẩu tạm thời
     */
    @PostMapping("/fetch-semesters")
    public ResponseEntity<ApiResponse<List<String>>> fetch(@RequestBody LoginRequest req) {
        List<String> semesters = timetableService.getSemesters(req.getMssv(), req.getPassword());

        return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                .status(200)
                .message("Lấy danh sách học kỳ thành công. Vui lòng chọn một kỳ học!")
                .data(semesters)
                .success(true)
                .build());
    }

    /**
     * API 2: Refresh dữ liệu từ Web trường theo học kỳ đã chọn
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(@RequestBody RefreshRequest req) {
        // Gọi service nhận về Object RefreshResponse
        RefreshResponse responseData = timetableService.refreshFromWeb(req.getMssv(), req.getSelectedSemester());

        String message = responseData.getTimetable().isEmpty()
                ? "Học kỳ này hiện chưa có dữ liệu thời khóa biểu."
                : "Cập nhật thành công!";

        return ResponseEntity.ok(ApiResponse.<RefreshResponse>builder()
                .status(200)
                .message(message)
                .data(responseData) // Bây giờ data sẽ có: { semesterStart: "...", timetable: [...] }
                .success(true)
                .build());
    }
    /**
     * API 3: Lấy dữ liệu đã lưu trong Database
     */
    @GetMapping("/{mssv}")
    public ResponseEntity<ApiResponse<List<TimeTableDaily>>> get(@PathVariable String mssv) {
        List<TimeTableDaily> data = timetableService.getDailyFromCache(mssv);

        String message = data.isEmpty() ? "Dữ liệu chưa được cập nhật. Vui lòng làm mới từ hệ thống!" : "Lấy dữ liệu thành công!";

        return ResponseEntity.ok(ApiResponse.<List<TimeTableDaily>>builder()
                .status(200)
                .message(message)
                .data(data)
                .success(true)
                .build());
    }
}