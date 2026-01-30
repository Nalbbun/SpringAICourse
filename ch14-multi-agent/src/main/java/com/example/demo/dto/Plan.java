package com.example.demo.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 여행 계획
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plan {
  // 일자별 여행 일정 목록
  private List<DaySchedule> days;

  // 최대 예산 (사용자가 입력한 예산, 단위: 원)
  private Integer maxBudget;

  // 총 예상 비용 (식비 + 숙박비 + 관광비 합계, 단위: 원)
  private Integer totalCost;

  // 식비 총액 (아침/점심/저녁 식사비 합계, 단위: 원)
  private Integer meals; 

  // 숙박비 총액 (전체 숙박 일수의 숙박비 합계, 단위: 원)
  private Integer accommodation;
    
  // 관광/입장료 총액 (모든 관광지 입장료 합계, 단위: 원)
  private Integer attractions;
}
