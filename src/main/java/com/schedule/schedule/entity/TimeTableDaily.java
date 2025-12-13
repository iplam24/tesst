package com.schedule.schedule.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "timetable_daily")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeTableDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mssv;

    private String maMon;
    private String tenMon;
    private Integer nhom;
    private Integer toNhom;
    private Integer soTinChi;
    private String lop;

    private Integer thu;
    private Integer tietBatDau;
    private Integer soTiet;
    private String phong;
    private String giangVien;

    private Integer tuanSo;     // tuần thứ mấy
    private LocalDate date;     // ngày thực tế của buổi học
}
