package com.schedule.schedule.service;

import com.schedule.schedule.dto.CrawlResult;
import com.schedule.schedule.dto.RefreshResponse;
import com.schedule.schedule.entity.TimeTableDaily;
import com.schedule.schedule.entity.TimeTableEntry;
import com.schedule.schedule.entity.UserCrawl;
import com.schedule.schedule.repository.TimeTableDailyRepository;
import com.schedule.schedule.repository.TimetableEntryRepository;
import com.schedule.schedule.repository.UserCrawlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableEntryRepository entryRepo;
    private final TimeTableDailyRepository dailyRepo;
    private final UserCrawlRepository userCrawlRepo;
    private final CrawlTKBService crawlTKBService;

    public List<String> getSemesters(String mssv, String password) {
        // Lưu/Cập nhật mật khẩu để API Refresh lấy ra dùng ngầm
        userCrawlRepo.save(new UserCrawl(mssv, password, LocalDateTime.now()));
        return crawlTKBService.getAvailableSemesters(mssv, password);
    }

    @Transactional
    public RefreshResponse refreshFromWeb(String mssv, String selectedSemester) {
        // 1. Lấy mật khẩu từ DB
        UserCrawl user = userCrawlRepo.findById(mssv)
                .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập lại!"));

        // 2. Gọi crawl (Hàm này đã trả về CrawlResult chứa cả ngày và html)
        CrawlResult result = crawlTKBService.crawlFinalData(mssv, user.getPassword(), selectedSemester);

        if (result.html() == null) {
            dailyRepo.deleteByMssv(mssv);
            entryRepo.deleteByMssv(mssv);
            return new RefreshResponse(result.semesterStart(), new ArrayList<>());
        }

        // 3. Parse và lưu trữ
        List<TimeTableEntry> entries = crawlTKBService.parseHtmlToEntries(result.html(), mssv);
        dailyRepo.deleteByMssv(mssv);
        entryRepo.deleteByMssv(mssv);
        entryRepo.saveAll(entries);

        List<TimeTableDaily> dailyList = crawlTKBService.expandToDaily(entries, result.semesterStart());
        List<TimeTableDaily> savedDaily = dailyRepo.saveAll(dailyList);

        // TRẢ VỀ ĐỐI TƯỢNG MỚI CHỨA NGÀY BẮT ĐẦU
        return new RefreshResponse(result.semesterStart(), savedDaily);
    }

    public List<TimeTableDaily> getDailyFromCache(String mssv) {
        return dailyRepo.findByMssv(mssv);
    }
}