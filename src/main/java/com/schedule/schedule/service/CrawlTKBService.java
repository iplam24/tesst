package com.schedule.schedule.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.schedule.schedule.dto.CrawlResult;
import com.schedule.schedule.entity.TimeTableDaily;
import com.schedule.schedule.entity.TimeTableEntry;
import lombok.RequiredArgsConstructor;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CrawlTKBService {

    /**
     * Login VNUA + lấy:
     *  - Ngày bắt đầu học kỳ (ngày bắt đầu "Tuần 1")
     *  - HTML TKB học kỳ (tab WEB_TKB_HK)
     */
    public CrawlResult crawlAll(String mssv, String password) {
        Playwright pw = Playwright.create();
        Browser browser = pw.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(true)
                        .setChannel("chrome")
                        .setSlowMo(350)
        );

        try {
            Page page = browser.newPage();

            // 1. LOGIN
            page.navigate("https://daotao.vnua.edu.vn/");
            page.waitForSelector("input[formcontrolname='username']");

            page.fill("input[formcontrolname='username']", mssv);
            page.fill("input[formcontrolname='password']", password);
            page.keyboard().press("Enter");


            page.waitForLoadState(LoadState.DOMCONTENTLOADED,
                    new Page.WaitForLoadStateOptions().setTimeout(15000));
            page.waitForTimeout(2000);  // Thêm 2s để Angular render alert


            Locator errorAlert = page.locator("div[role='alert'].alert.alert-danger");


            int alertCount = errorAlert.count();
            if (alertCount > 0) {
                String alertText = errorAlert.first().innerText().trim();
                System.out.println("PHÁT HIỆN ALERT LỖI: " + alertText + " (cho MSSV: " + mssv + ")");

                if (alertText.contains("Đăng nhập không thành công")) {
                    throw new RuntimeException("SAI_TAI_KHOAN_HOAC_MAT_KHAU");
                } else {
                    throw new RuntimeException("DANG_NHAP_LOI: " + alertText);
                }
            }

            // 2. TKB TUẦN – LẤY TUẦN 1
            page.click("#WEB_TKB_1TUAN");
            page.waitForTimeout(1200);

            page.waitForSelector("ng-select[bindlabel='thong_tin_tuan']",
                    new Page.WaitForSelectorOptions().setTimeout(30000));

            Locator weekDropdown = page.locator(
                    "ng-select[bindlabel='thong_tin_tuan'] .ng-select-container"
            );

            // đợi dropdown sẵn sàng
            weekDropdown.waitFor(new Locator.WaitForOptions().setTimeout(15000));

            page.waitForTimeout(700);

            // MỞ DROPDOWN CHẮC CHẮN
            boolean opened = false;
            for (int i = 0; i < 4; i++) {
                try {
                    weekDropdown.scrollIntoViewIfNeeded();
                    weekDropdown.hover();
                    page.waitForTimeout(250);

                    weekDropdown.click(new Locator.ClickOptions().setForce(true));

                    page.waitForSelector(".ng-dropdown-panel .ng-option",
                            new Page.WaitForSelectorOptions().setTimeout(5000));

                    if (page.locator(".ng-dropdown-panel").isVisible()) {
                        opened = true;
                        break;
                    }

                } catch (Exception e) {
                    System.out.println("Retry mở dropdown tuần lần " + (i + 1));
                    page.waitForTimeout(800);
                }
            }

            if (!opened) throw new RuntimeException("KHONG_MO_DUOC_DROPDOWN_TUAN");

            // Scroll lên đầu panel
            Locator panel = page.locator(".ng-dropdown-panel .ng-dropdown-panel-items").first();
            panel.evaluate("el => el.scrollTop = 0");
            page.waitForTimeout(500);

            String firstOptionText = page.locator(".ng-dropdown-panel .ng-option")
                    .first()
                    .innerText()
                    .trim();

            System.out.println("Option Tuần 1: " + firstOptionText);

            Pattern pattern = Pattern.compile("từ ngày (\\d{2}/\\d{2}/\\d{4})");
            Matcher matcher = pattern.matcher(firstOptionText);

            if (!matcher.find()) {
                throw new RuntimeException("KHONG_PARSE_DUOC_NGAY_BAT_DAU_TU_OPTION: " + firstOptionText);
            }

            LocalDate semesterStart = LocalDate.parse(matcher.group(1),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            System.out.println(">>> Ngày bắt đầu học kỳ (Tuần 1): " + semesterStart);

            // đóng dropdown
            page.keyboard().press("Escape");
            page.waitForTimeout(500);

            // 3. VÀO TKB HỌC KỲ
            page.click("#WEB_TKB_HK");
            page.waitForTimeout(1500);

            page.waitForSelector("#excel-table tbody tr",
                    new Page.WaitForSelectorOptions().setTimeout(45000));

            // lấy html
            String html = page.innerHTML("#printArea");

            return new CrawlResult(semesterStart, html);

        } catch (PlaywrightException e) {
            throw new RuntimeException("CRAWL_FAILED: " + e.getMessage(), e);
        } finally {
            browser.close();
            pw.close();
        }
    }

    /**
     * Parse HTML thành list TimeTableEntry
     */
    public List<TimeTableEntry> parseHtmlToEntries(String html, String mssv) {
        List<TimeTableEntry> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        Document doc = Jsoup.parse(html);
        Element table = doc.selectFirst("table#excel-table");

        if (table == null) {
            return result;
        }

        Elements rows = table.select("tbody > tr");

        // lưu các giá trị rowspan
        String maMon = null;
        String tenMon = null;
        Integer nhom = null;
        Integer toNhom = null;
        Integer soTinChi = null;
        Integer soTietLT = null;
        Integer soTietTH = null;
        String lop = null;

        for (Element row : rows) {
            Elements cols = row.select("td");
            if (cols.isEmpty()) continue;

            boolean isFirstRow = !cols.get(0).attr("rowspan").isEmpty();

            if (isFirstRow) {
                // ===== dòng đầu môn học =====
                maMon = text(cols, 0);
                tenMon = text(cols, 1);

                Integer[] nt = parseNhomTo(text(cols, 2));
                nhom = nt[0];
                toNhom = nt[1];

                soTinChi = parseIntSafe(text(cols, 3));
                soTietLT = parseIntSafe(text(cols, 4));
                soTietTH = parseIntSafe(text(cols, 5));
                lop = text(cols, 6);

                Integer thu = parseThu(text(cols, 8));
                Integer tietBD = parseIntSafe(text(cols, 9));
                Integer soTiet = parseIntSafe(text(cols, 10));
                String phong = text(cols, 11);
                String gv = text(cols, 12);
                String chuoiTuan = text(cols, 13);

                if (thu != null && tietBD != null) {
                    result.add(TimeTableEntry.builder()
                            .mssv(mssv)
                            .maMon(maMon)
                            .tenMon(tenMon)
                            .nhom(nhom)
                            .toNhom(toNhom)
                            .soTinChi(soTinChi)
                            .soTietLT(soTietLT)
                            .soTietTH(soTietTH)
                            .lop(lop)
                            .thu(thu)
                            .tietBatDau(tietBD)
                            .soTiet(soTiet)
                            .phong(phong)
                            .giangVien(gv)
                            .chuoiTuan(chuoiTuan)
                            .lastUpdated(now)
                            .build()
                    );
                }

            } else {
                // ===== các dòng tiếp theo =====
                if (maMon == null) continue;

                Integer thu = parseThu(text(cols, 0));
                Integer tietBD = parseIntSafe(text(cols, 1));
                Integer soTiet = parseIntSafe(text(cols, 2));
                String phong = text(cols, 3);
                String gv = text(cols, 4);
                String chuoiTuan = text(cols, 5);

                if (thu != null && tietBD != null) {
                    result.add(TimeTableEntry.builder()
                            .mssv(mssv)
                            .maMon(maMon)
                            .tenMon(tenMon)
                            .nhom(nhom)
                            .toNhom(toNhom)
                            .soTinChi(soTinChi)
                            .soTietLT(soTietLT)
                            .soTietTH(soTietTH)
                            .lop(lop)
                            .thu(thu)
                            .tietBatDau(tietBD)
                            .soTiet(soTiet)
                            .phong(phong)
                            .giangVien(gv)
                            .chuoiTuan(chuoiTuan)
                            .lastUpdated(now)
                            .build()
                    );
                }
            }
        }

        return result;
    }

    // Helpers
    private String text(Elements c, int i) {
        return c.size() > i ? c.get(i).text().trim() : "";
    }

    private Integer parseIntSafe(String s) {
        try {
            return (s == null || s.isBlank()) ? null : Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseThu(String s) {
        if (s == null || s.isBlank()) return null;
        if (s.equalsIgnoreCase("CN")) return 8;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer[] parseNhomTo(String str) {
        if (str == null || str.isBlank()) return new Integer[]{null, null};
        String[] p = str.split("-");
        Integer a = null, b = null;
        try {
            if (p.length > 0) a = Integer.parseInt(p[0].trim());
        } catch (Exception ignored) {}
        try {
            if (p.length > 1) b = Integer.parseInt(p[1].trim());
        } catch (Exception ignored) {}
        return new Integer[]{a, b};
    }

    public List<TimeTableDaily> expandToDaily(
            List<TimeTableEntry> entries,
            LocalDate semesterStart
    ) {
        List<TimeTableDaily> result = new ArrayList<>();

        for (TimeTableEntry e : entries) {
            String weeks = e.getChuoiTuan();
            if (weeks == null) continue;

            for (int i = 0; i < weeks.length(); i++) {
                char c = weeks.charAt(i);
                if (c < '1' || c > '9') continue;

                int weekIndex = i + 1;

                LocalDate monday = semesterStart.plusWeeks(weekIndex - 1);
                LocalDate date = monday.plusDays(e.getThu() - 2);

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

        return result;
    }

}
