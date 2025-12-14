package com.schedule.schedule.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.schedule.schedule.dto.CrawlResult;
import com.schedule.schedule.entity.TimeTableDaily;
import com.schedule.schedule.entity.TimeTableEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlTKBService {

    public List<String> getAvailableSemesters(String mssv, String password) {
        try (Playwright pw = Playwright.create();

             // Trong hàm crawl hoặc login
             // Sửa đoạn Browser browser = ...
             Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions()
                     .setHeadless(true) // Bắt buộc phải là true
                     .setArgs(List.of(
                             "--no-sandbox",
                             "--disable-setuid-sandbox",
                             "--disable-dev-shm-usage", // Sử dụng bộ nhớ thay vì file tạm /dev/shm
                             "--disable-gpu",            // Tắt tăng tốc phần cứng
                             "--single-process",         // Chạy trên 1 tiến trình duy nhất (tiết kiệm RAM)
                             "--disable-extensions"      // Tắt các tiện ích mở rộng
                     )));
             Page page = browser.newPage()) {

            login(page, mssv, password);
            handleInitialPopup(page);

            page.click("#WEB_TKB_1TUAN");
            Locator dropdown = page.locator("ng-select[bindlabel='ten_hoc_ky'] .ng-select-container");
            dropdown.waitFor();
            dropdown.click();

            page.waitForCondition(() -> {
                Locator options = page.locator(".ng-dropdown-panel .ng-option");
                return options.count() > 0 && options.first().innerText().contains("Học kỳ");
            }, new Page.WaitForConditionOptions().setTimeout(10000));

            List<String> rawOptions = page.locator(".ng-dropdown-panel .ng-option").allInnerTexts();
            page.keyboard().press("Escape");

            return rawOptions.stream()
                    .map(this::extractSemester)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }
    }

    public CrawlResult crawlFinalData(String mssv, String password, String selectedSemester) {
        try (Playwright pw = Playwright.create();
             // Trong hàm crawl hoặc login
             // Sửa đoạn Browser browser = ...
             Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions()
                     .setHeadless(true) // Bắt buộc phải là true
                     .setArgs(List.of(
                             "--no-sandbox",
                             "--disable-setuid-sandbox",
                             "--disable-dev-shm-usage", // Sử dụng bộ nhớ thay vì file tạm /dev/shm
                             "--disable-gpu",            // Tắt tăng tốc phần cứng
                             "--single-process",         // Chạy trên 1 tiến trình duy nhất (tiết kiệm RAM)
                             "--disable-extensions"      // Tắt các tiện ích mở rộng
                     )));
             Page page = browser.newPage()) {

            login(page, mssv, password);
            handleInitialPopup(page);

            // --- BƯỚC 1: TKB TUẦN ---
            page.click("#WEB_TKB_1TUAN");
            page.waitForTimeout(1500);

            // A. Chọn đúng Học kỳ
            Locator semesterDropdown = page.locator("ng-select[bindlabel='ten_hoc_ky'] .ng-select-container");
            semesterDropdown.click();
            page.locator(".ng-dropdown-panel .ng-option")
                    .filter(new Locator.FilterOptions().setHasText(selectedSemester))
                    .first()
                    .click();

            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(800);

            // B. Mở dropdown Thông tin tuần
            Locator weekDropdown = page.locator("ng-select[bindlabel='thong_tin_tuan'] .ng-select-container");
            weekDropdown.click();

            // C. Đợi panel tuần render xong
            Locator weekPanel = page.locator(".ng-dropdown-panel-items.scroll-host");
            weekPanel.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

            // D. Scroll lên đầu (sử dụng mouse wheel để trigger virtual scroll)
            for (int i = 0; i < 8; i++) {
                weekPanel.hover();
                page.mouse().wheel(0, -1000);
                page.waitForTimeout(120);
            }

            // E. Lấy tuần đầu tiên sau khi scroll
            Locator firstWeek = page.locator(".ng-dropdown-panel .ng-option").first();
            String weekText = firstWeek.innerText();

            // Đóng dropdown
            page.keyboard().press("Escape");

            // Parse ngày bắt đầu học kỳ từ tuần đầu
            LocalDate semesterStart = parseDate(weekText);

            System.out.println("Ngày đầu tiên của kì là: "+ semesterStart);

            // --- BƯỚC 2: TKB HỌC KỲ ---
// Click vào tab TKB Học kỳ
            page.click("#WEB_TKB_HK", new Page.ClickOptions().setNoWaitAfter(true));
            page.waitForTimeout(500);

// Mở dropdown chọn học kỳ

            Locator hkCombo = page.locator("ng-select[bindlabel=\"ten_hoc_ky\"] .ng-select-container");
            hkCombo.click();


// Đợi panel xuất hiện
            page.waitForSelector(".ng-dropdown-panel .ng-option", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

// Click vào học kỳ mong muốn
            page.locator(".ng-dropdown-panel .ng-option")
                    .filter(new Locator.FilterOptions().setHasText(selectedSemester))
                    .first()
                    .click();

            page.waitForTimeout(1000);

// 1. Chờ bảng xuất hiện trong DOM
            page.waitForSelector("#excel-table tbody tr", new Page.WaitForSelectorOptions().setTimeout(30000));

// 2. Đợi thêm 1 nhịp để Angular hoàn tất đổ dữ liệu (Binding)
// Đây là "bí kíp" để tránh bốc phải bảng trống hoặc bảng cũ
            page.waitForTimeout(500);

// 3. Kiểm tra trạng thái "Không tìm thấy dữ liệu"
            Locator noDataMsg = page.locator("#excel-table tbody tr td[colspan='100']");

            if (noDataMsg.isVisible() && noDataMsg.innerText().contains("Không tìm thấy dữ liệu")) {
                System.out.println(">>> Kết quả: Học kỳ này thực sự không có lịch học.");
                return new CrawlResult(semesterStart, null);
            }

// 4. Nếu không rỗng, lấy HTML từ #printArea (Đảm bảo chuẩn Element bạn gửi)
            String html = page.innerHTML("#printArea");
            System.out.println(">>> Kết quả: Đã bốc thành công HTML Thời khóa biểu.");

            return new CrawlResult(semesterStart, html);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi crawl chi tiết: " + e.getMessage(), e);
        }
    }


    private void login(Page page, String mssv, String password) {
        page.navigate("https://daotao.vnua.edu.vn/");
        page.fill("input[formcontrolname='username']", mssv);
        page.fill("input[formcontrolname='password']", password);
        page.keyboard().press("Enter");
        page.setDefaultTimeout(60000);
        page.waitForSelector(".alert-danger, #WEB_TKB_1TUAN", new Page.WaitForSelectorOptions().setTimeout(15000));
        if (page.locator(".alert-danger").isVisible()) throw new RuntimeException("SAI_TAI_KHOAN");
    }

    private void handleInitialPopup(Page page) {
        try {
            Locator closeBtn = page.locator("#btn_tb_close");
            closeBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(1500));
            closeBtn.click();
        } catch (Exception ignored) {}
    }

    private String extractSemester(String text) {
        int index = text.indexOf("Học kỳ");
        return (index != -1) ? text.substring(index).trim() : null;
    }

    private LocalDate parseDate(String text) {
        Matcher m = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})").matcher(text);
        if (m.find()) return LocalDate.parse(m.group(1), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        throw new RuntimeException("Lỗi parse ngày");
    }

    public List<TimeTableEntry> parseHtmlToEntries(String html, String mssv) {
        List<TimeTableEntry> result = new ArrayList<>();
        if (html == null || html.isBlank()) return result;

        Document doc = Jsoup.parse(html);
        Element table = doc.selectFirst("table#excel-table");
        if (table == null) return result;

        Elements rows = table.select("tbody > tr");

        // Khởi tạo các biến nhớ để xử lý rowspan
        String currentMaMon = "", currentTenMon = "", currentLop = "";
        Integer currentNhom = null, currentToNhom = null, currentTinChi = null;

        for (Element row : rows) {
            Elements cols = row.select("td");

            // BỎ QUA các dòng không có dữ liệu hoặc dòng thông báo rỗng
            if (cols.isEmpty() || cols.get(0).text().contains("Không tìm thấy dữ liệu")) {
                continue;
            }

            // KIỂM TRA DÒNG GỐC (Dòng có chứa Mã môn học - thường có rowspan)
            if (!cols.get(0).attr("rowspan").isEmpty()) {
                if (cols.size() < 13) continue; // Đảm bảo đủ số cột tối thiểu cho dòng chính

                currentMaMon = cols.get(0).text().trim();
                currentTenMon = cols.get(1).text().trim();

                // Xử lý Nhóm tổ (ví dụ: 02-02)
                String nhomToText = cols.get(2).text().trim();
                if (nhomToText.contains("-")) {
                    String[] nt = nhomToText.split("-");
                    currentNhom = parseIntSafe(nt[0]);
                    currentToNhom = nt.length > 1 ? parseIntSafe(nt[1]) : null;
                } else {
                    currentNhom = parseIntSafe(nhomToText);
                    currentToNhom = null;
                }

                currentTinChi = parseIntSafe(cols.get(3).text());
                currentLop = cols.get(6).text().trim();

                // Cột dữ liệu thời gian bắt đầu từ index 8 cho dòng chính
                addEntrySafe(result, mssv, currentMaMon, currentTenMon, currentNhom, currentToNhom, currentTinChi, currentLop, cols, 8);
            }
            // KIỂM TRA DÒNG PHỤ (Dòng con của rowspan - không có các cột đầu)
            else {
                // Dòng phụ thường bắt đầu ngay bằng Thứ (Index 0), Tiết (Index 1)...
                // Cần tối thiểu 6 cột (Thứ, Tiết BD, Số tiết, Phòng, GV, Tuần)
                if (cols.size() >= 6) {
                    addEntrySafe(result, mssv, currentMaMon, currentTenMon, currentNhom, currentToNhom, currentTinChi, currentLop, cols, 0);
                }
            }
        }
        return result;
    }

    private void addEntrySafe(List<TimeTableEntry> list, String mssv, String ma, String ten, Integer n, Integer t, Integer tc, String l, Elements cols, int offset) {
        // Kiểm tra an toàn: offset + 5 là cột cuối cùng cần lấy (Chuỗi tuần)
        if (cols.size() <= offset + 5) return;

        try {
            Integer thu = parseThu(cols.get(offset).text().trim());
            Integer tietBD = parseIntSafe(cols.get(offset + 1).text().trim());
            Integer soTiet = parseIntSafe(cols.get(offset + 2).text().trim());
            String phong = cols.get(offset + 3).text().trim();
            String gv = cols.get(offset + 4).text().trim();
            String chuoiTuan = cols.get(offset + 5).text().trim();

            if (thu != null && tietBD != null) {
                list.add(TimeTableEntry.builder()
                        .mssv(mssv).maMon(ma).tenMon(ten).nhom(n).toNhom(t).soTinChi(tc).lop(l)
                        .thu(thu).tietBatDau(tietBD).soTiet(soTiet)
                        .phong(phong).giangVien(gv).chuoiTuan(chuoiTuan)
                        .lastUpdated(LocalDateTime.now()).build());
            }
        } catch (Exception ex) {
            log.warn("Bỏ qua 1 dòng do lỗi parse dữ liệu: {}", ex.getMessage());
        }
    }
    private Integer parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    private Integer parseThu(String s) {
        if (s == null || s.isBlank()) return null;
        if (s.equalsIgnoreCase("CN")) return 8;
        return parseIntSafe(s);
    }

    public List<TimeTableDaily> expandToDaily(List<TimeTableEntry> entries, LocalDate semesterStart) {
        List<TimeTableDaily> result = new ArrayList<>();

        for (TimeTableEntry e : entries) {
            String weeks = e.getChuoiTuan();
            if (weeks == null || weeks.isEmpty()) continue;

            for (int i = 0; i < weeks.length(); i++) {
                char c = weeks.charAt(i);
                // Nếu tại vị trí i là con số (tuần đó có học)
                if (Character.isDigit(c)) {
                    int weekIndex = i + 1; // Tuần thứ mấy

                    // Ngày thứ 2 của tuần đó = Ngày bắt đầu kỳ + (số tuần - 1)
                    LocalDate mondayOfWeek = semesterStart.plusWeeks(weekIndex - 1);
                    // Ngày học thực tế = Thứ 2 + (Thứ trong tuần - 2)
                    // (Ví dụ: Thứ 3 thì cộng thêm 1 ngày vào Thứ 2)
                    LocalDate date = mondayOfWeek.plusDays(e.getThu() - 2);

                    result.add(TimeTableDaily.builder()
                            .mssv(e.getMssv())
                            .maMon(e.getMaMon())
                            .tenMon(e.getTenMon())
                            .nhom(e.getNhom())
                            .toNhom(e.getToNhom())
                            .soTinChi(e.getSoTinChi())
                            .lop(e.getLop())
                            .thu(e.getThu())
                            .tietBatDau(e.getTietBatDau())
                            .soTiet(e.getSoTiet())
                            .phong(e.getPhong())
                            .giangVien(e.getGiangVien())
                            .tuanSo(weekIndex)
                            .date(date)
                            .build());
                }
            }
        }
        return result;
    }
}