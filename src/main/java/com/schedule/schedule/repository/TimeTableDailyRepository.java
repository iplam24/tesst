package com.schedule.schedule.repository;

import com.schedule.schedule.entity.TimeTableDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimeTableDailyRepository extends JpaRepository<TimeTableDaily, Long> {
    List<TimeTableDaily> findByMssvOrderByDateAsc(String mssv);
    // Lấy toàn bộ TKB theo ngày của 1 MSSV

    void deleteByMssv(String mssv);
    // Nếu muốn lọc theo khoảng ngày

    List<TimeTableDaily> findByMssv(String mssv);
}
