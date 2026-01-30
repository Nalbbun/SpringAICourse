package com.example.demo.dto;

import java.util.List;

import lombok.Data;

@Data
public class PlanState {
    // 1. 요구사항: 조율자가 설정하는 기본 정보
    private String userQuery;        // 원본 사용자 질문
    private String destination;      // 확정된 여행지
    private Integer days;            // 여행 기간
    private Integer maxBudget;       // 사용자 제한 예산
    
    // 2. 전문가 작업 영역: 각 에이전트가 채워넣는 데이터
    private List<Attraction> attractions;       // 관광지 에이전트의 결과
    private List<Restaurant> restaurants;       // 맛집 에이전트의 결과
    private List<Accommodation> accommodations; // 숙소 에이전트의 결과
    
    // 3. 분석 및 산출물: 데이터 통합 후의 결과물
    private BudgetAnalysis budgetAnalysis;      // 예산 에이전트의 분석 리포트
    private Plan plan;                          // 플랜 에이전트가 만든 최종 일정표
    
    // 4. 흐름 제어: 피드백 루프를 위한 상태 값
    private boolean replan;                     // 예산 초과 등으로 인한 재작성 여부
    private Integer previousTotalCost;          // 재계획 시 참고할 직전 총 비용
}
