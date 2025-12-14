package com.schedule.schedule.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// UserCrawl.java
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCrawl {
    @Id
    private String mssv;
    private String password;
    private LocalDateTime lastUpdate;
}