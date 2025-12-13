package com.schedule.schedule.repository;

import com.schedule.schedule.entity.TimeTableEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimetableEntryRepository extends JpaRepository<TimeTableEntry, Long> {

    List<TimeTableEntry> findByMssvOrderByThuAscTietBatDauAsc(String mssv);

    void deleteByMssv(String mssv);

    List<TimeTableEntry> findByMssv(String mssv);
}