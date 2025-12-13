package com.schedule.schedule.dto;

import java.time.LocalDate;

public record CrawlResult(LocalDate semesterStart, String html) {}
