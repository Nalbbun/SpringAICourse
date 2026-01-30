package com.example.demo.dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

// 일자별 여행 일정
@Data
@NoArgsConstructor
public class DaySchedule {
    // 일자 번호 (1일차, 2일차, ...)
    private int day;
    
    // 하루 일정 목록 (시간순으로 정렬)
    private List<ScheduleItem> schedule;
}

