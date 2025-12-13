package com.schedule.schedule.controller;

import com.schedule.schedule.dto.ApiResponse;
import com.schedule.schedule.dto.TimeTableEntryDTO;
import com.schedule.schedule.entity.TimeTableDaily;
import com.schedule.schedule.entity.TimeTableEntry;
import com.schedule.schedule.service.TimetableService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tkb")
@RequiredArgsConstructor
public class TimeTableController {

    private final TimetableService timetableService;

    /**
     * FE gửi MSSV + mật khẩu để BE crawl lại TKB
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<?>> refresh(@RequestBody RefreshRequest req) {
        try {
            List<TimeTableDaily> data = timetableService.refreshFromWeb(
                    req.getMssv(),
                    req.getPassword()
            );

            return ResponseEntity.ok(ApiResponse.builder()
                    .status(HttpStatus.OK.value())
                    .success(true)
                    .message("Cập nhật thời khóa biểu thành công!")
                    .data(data)
                    .build());

        } catch (RuntimeException e) {
            String rawMsg = e.getMessage() != null ? e.getMessage().trim() : "";

            // 1. SAI TÀI KHOẢN HOẶC MẬT KHẨU
            if ("SAI_TAI_KHOAN_HOAC_MAT_KHAU".equals(rawMsg)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.builder()
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .success(false)
                                .message("Tài khoản hoặc mật khẩu không chính xác. Vui lòng kiểm tra lại!")
                                .build());
            }

            // 2. Hệ thống trường quá tải
            if (rawMsg.contains("Timeout") || rawMsg.contains("NETWORK") || rawMsg.contains("daotao.vnua.edu.vn")) {
                return ResponseEntity
                        .status(HttpStatus.GATEWAY_TIMEOUT)
                        .body(ApiResponse.builder()
                                .status(HttpStatus.GATEWAY_TIMEOUT.value())
                                .success(false)
                                .message("Hệ thống đào tạo VNUA đang quá tải. Vui lòng thử lại sau ít phút.")
                                .build());
            }

            // 3. Lỗi kỹ thuật (Parse/Dropdown)
            if (rawMsg.contains("DROPDOWN") || rawMsg.contains("PARSE")) {
                return ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ApiResponse.builder()
                                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                                .success(false)
                                .message("Lỗi kỹ thuật khi lấy dữ liệu. Vui lòng liên hệ Admin.")
                                .build());
            }

            // 4. Lỗi khác
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .success(false)
                            .message("Có lỗi bất ngờ xảy ra: " + rawMsg)
                            .build());

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .success(false)
                            .message("Lỗi hệ thống nghiêm trọng.")
                            .build());
        }
    }



    /**
     * Lấy TKB đã lưu trong DB
     */
    @GetMapping("/{mssv}")
    public List<TimeTableDaily> getTKB(@PathVariable String mssv) {
        return timetableService.getDailyFromCache(mssv);
    }

    @GetMapping
    public List<TimeTableEntryDTO> getByMssv(@RequestParam String mssv) {
        return timetableService.getByMssv(mssv);
    }

    @Data
    public static class RefreshRequest {
        private String mssv;
        private String password;
    }
}
