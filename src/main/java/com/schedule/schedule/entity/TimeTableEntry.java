package com.schedule.schedule.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "timetable_entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeTableEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MSSV
    @Column(nullable = false, length = 50)
    private String mssv;

    // Thông tin môn học
    private String maMon;       // TH03117
    private String tenMon;      // HT hoạch định nguồn lực DN
    private Integer nhom;       // 2
    private Integer toNhom;     // 2
    private Integer soTinChi;   // 3
    private Integer soTietLT;   // 30 (có thể null)
    private Integer soTietTH;   // 15 (có thể null)
    private String lop;         // K67CNPMB

    // Lịch học
    private Integer thu;        // 2..7, 8 = CN
    private Integer tietBatDau; // 1..n
    private Integer soTiet;     // 1..n
    private String phong;       // E204, THCNTT92,...
    private String giangVien;   // có thể rỗng
    private String chuoiTuan;   // "123456----------------"

    private LocalDateTime lastUpdated;
}
