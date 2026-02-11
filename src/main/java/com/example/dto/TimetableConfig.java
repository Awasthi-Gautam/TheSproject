package com.example.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimetableConfig {
    private int periodsPerDay = 8;
    private int daysPerWeek = 5;
    private int breakSlot = 4;
}
