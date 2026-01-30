package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 예산 분석 결과
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetAnalysis {
    // 최대 예산
    private Integer maxBudget;

    // 총 경비 
    private Integer totalCost;    
        
    // 예산 초과 여부 (true: 초과, false: 예산 내 또는 예산 미설정)
    private boolean exceeded;

    // 예산 분석 메시지 (예: "예산 초과: 100,000원", "예산 내 사용: 50,000원 남음")
    private String message;    
}
