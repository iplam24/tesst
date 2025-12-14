package com.schedule.schedule.dto;

import com.schedule.schedule.entity.TimeTableDaily;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshResponse {
    private LocalDate semesterStart;
    private List<TimeTableDaily> timetable;
}