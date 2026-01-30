package com.example.demo.agent;

import org.springframework.stereotype.Component;

import com.example.demo.dto.BudgetAnalysis;
import com.example.demo.dto.DaySchedule;
import com.example.demo.dto.Plan;
import com.example.demo.dto.PlanState;
import com.example.demo.dto.ScheduleItem;

// 예산 분석 에이전트
@Component
public class BudgetAgent {
  // 예산 분석 수행
  public void execute(PlanState state) {
    // 사용자 예산 및 생성된 여행 초기 계획 얻기
    int maxBudget = state.getMaxBudget();
    Plan plan = state.getPlan();
    
    // 총 비용 계산
    calculateAndUpdateCategoryCosts(plan);
    
    // Plan에서 총 비용 얻기
    int actualTotalCost = plan.getTotalCost();
    
    // 초과 여부 계산
    boolean exceeded = actualTotalCost > maxBudget;
    
    // 예산 검증 메시지 생성
    String message = String.format(
        "총 비용: %,d원 | 예산: %,d원 | %s",
        actualTotalCost, maxBudget,
        exceeded ? "⚠️ 초과" : "✅ 정상"
    );
    // 예산 분석 결과 저장
    state.setBudgetAnalysis(new BudgetAnalysis(
      maxBudget, actualTotalCost, exceeded, message));
  }
  
  //-----------------------------------------------------------------------------------
  // 관광지, 맛집, 숙소별 총 비용 및 전체 총 비용 계산 후 Plan에 저장
  private void calculateAndUpdateCategoryCosts(Plan plan) {
    int mealsCost = 0;
    int accommodationCost = 0;
    int attractionsCost = 0;
    
    if (plan.getDays() != null) {
      for (DaySchedule day : plan.getDays()) {
        if (day.getSchedule() != null) {
          for (ScheduleItem item : day.getSchedule()) {
            String type = item.getType();
            int cost = item.getCost();
            
            if ("meal".equals(type)) {
              mealsCost += cost;
            } else if ("accommodation".equals(type)) {
              accommodationCost += cost;
            } else if ("attraction".equals(type)) {
              attractionsCost += cost;
            }
          }
        }
      }
    }
    
    // Plan 객체에 카테고리별 비용 저장
    plan.setMeals(mealsCost);
    plan.setAccommodation(accommodationCost);
    plan.setAttractions(attractionsCost);
    
    // 총 비용 계산후 저장
    int totalCost = mealsCost + accommodationCost + attractionsCost;
    plan.setTotalCost(totalCost);
  }
}
