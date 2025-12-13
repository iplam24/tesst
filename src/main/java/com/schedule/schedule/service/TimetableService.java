package com.schedule.schedule.service;

import com.schedule.schedule.dto.CrawlResult;
import com.schedule.schedule.dto.TimeTableEntryDTO;
import com.schedule.schedule.entity.TimeTableDaily;
import com.schedule.schedule.entity.TimeTableEntry;
import com.schedule.schedule.mapper.TimeTableMapper;
import com.schedule.schedule.repository.TimeTableDailyRepository;
import com.schedule.schedule.repository.TimetableEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableEntryRepository entryRepo;
    private final TimeTableDailyRepository dailyRepo;
    private final CrawlTKBService crawlTKBService;

    /**
     * GET từ DB
     */
    public List<TimeTableEntry> getFromCache(String mssv) {
        return entryRepo.findByMssvOrderByThuAscTietBatDauAsc(mssv);
    }

    /**
     * Gọi FE
     */
    public List<TimeTableEntryDTO> getByMssv(String mssv) {
        return entryRepo.findByMssv(mssv)
                .stream()
                .map(TimeTableMapper::toDTO)
                .toList();
    }

    public List<TimeTableDaily> getDailyFromCache(String mssv) {
        return dailyRepo.findByMssv(mssv);
        // nếu anh có method sort sẵn thì dùng:
        // return dailyRepo.findByMssvOrderByNgayHocAscThuAscTietBatDauAsc(mssv);
    }


    /**
     * CRAWL FULL:
     * - login đúng 1 lần
     * - lấy ngày bắt đầu kỳ
     * - lấy tkb học kỳ
     * - parse entry
     * - expand daily
     * - lưu DB
     */
    @Transactional
    public List<TimeTableDaily> refreshFromWeb(String mssv, String password) {

        // 1. Crawl 1 phát ra cả ngày bắt đầu + html
        CrawlResult result = crawlTKBService.crawlAll(mssv, password);

        // 2. Parse entry
        List<TimeTableEntry> entries =
                crawlTKBService.parseHtmlToEntries(result.html(), mssv);

        // 3. Xoá dữ liệu cũ
        dailyRepo.deleteByMssv(mssv);
        entryRepo.deleteByMssv(mssv);

        // 4. Lưu entry gốc
        entryRepo.saveAll(entries);

        // 5. Sinh daily từ tuần → ngày cụ thể
        List<TimeTableDaily> daily =
                crawlTKBService.expandToDaily(entries, result.semesterStart());

        // 6. Lưu daily
        return dailyRepo.saveAll(daily);
    }
}
