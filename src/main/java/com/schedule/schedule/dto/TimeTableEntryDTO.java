package com.schedule.schedule.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TimeTableEntryDTO {
    private Long id;
    private String mssv;
    private String maMon;
    private String tenMon;
    private Integer nhom;
    private Integer toNhom;
    private Integer soTinChi;
    private Integer soTietLT;
    private Integer soTietTH;
    private String lop;
    private Integer thu;         // 2..7, CN anh đang để 8
    private Integer tietBatDau;
    private Integer soTiet;
    private String phong;
    private String giangVien;
    private String chuoiTuan;
    private LocalDateTime lastUpdated;

    // getter / setter
}
