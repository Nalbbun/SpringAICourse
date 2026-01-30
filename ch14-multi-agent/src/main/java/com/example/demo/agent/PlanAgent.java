package com.example.demo.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import com.example.demo.dto.Accommodation;
import com.example.demo.dto.Attraction;
import com.example.demo.dto.Plan;
import com.example.demo.dto.PlanState;
import com.example.demo.dto.Restaurant;

import lombok.extern.slf4j.Slf4j;

// 여행 일정 계획 에이전트: 수집된 정보를 바탕으로 전체 여행 일정 작성
@Component
@Slf4j
public class PlanAgent {
  //-----------------------------------------------------------------------------------
  // 시스템 프롬프트
  private static final String SYSTEM_PROMPT = """
      당신은 여행 일정을 계획하는 전문 에이전트입니다.
      주어진 여행 정보와 장소 목록을 바탕으로 실용적이고 균형잡힌 여행 일정을 작성합니다.
      예산을 고려하면서도 여행자가 충분히 즐길 수 있도록 다양한 장소를 선택합니다.
      """;
  
  //-----------------------------------------------------------------------------------
  // 사용자 프롬프트 템플릿
  private static final String USER_PROMPT_TEMPLATE = """
      제주도 {days}일 여행 일정을 작성해주세요.
      
      ## 여행 정보
      - 여행 기간: {days}일
      - 총 예산: {budget}원
      
      ## 추천 관광지 목록
      {attractions}
      
      ## 추천 맛집 목록
      {restaurants}
      
      ## 추천 숙소 목록
      {accommodations}
      
      ## 일정 작성 규칙
      1. 매일 포함할 항목:
         - 오전 관광지 1-2곳 (09:00-12:00)
         - 점심 식사 (12:00-13:00) - 맛집에서 선택
         - 오후 관광지 1-2곳 (14:00-18:00)
         - 저녁 식사 (18:00-19:00) - 맛집에서 선택
         - 숙소 체크인 (20:00) - 마지막 날 제외
      
      2. 숙박 규칙:
         - {days}일 여행 = {nights}박
         - 마지막 날에는 숙소가 필요 없음
      
      3. 중복 방지 규칙 (매우 중요):
         - 같은 관광지는 전체 일정에서 단 1번만 방문
         - 같은 맛집은 전체 일정에서 단 1번만 방문 (점심과 저녁에 다른 맛집 선택)
         - 같은 숙소는 전체 일정에서 단 1번만 사용
         - 각 날짜의 점심과 저녁은 반드시 다른 맛집을 선택
      
      4. 일정 작성 형식:
         - 각 일정 항목에 반드시 포함: time, type, name, address, description, cost
         - type은 정확히: 'attraction', 'meal', 'accommodation'
         - time은 HH:mm 형식 (예: 09:00, 12:30)
         - name: 위 목록의 이름을 정확히 사용
         - address: 위 목록의 주소를 그대로 복사
         - description: 위 목록의 설명을 그대로 복사
         - cost: 위 목록의 비용을 그대로 사용 (숫자만, 원 단위 생략)
         - 식사는 '점심 - 식당이름' 또는 '저녁 - 식당이름' 형식으로 작성
      """;
  
  //-----------------------------------------------------------------------------------
  private final ChatClient chatClient;

  //-----------------------------------------------------------------------------------
  // 생성자: ChatClient 초기화
  public PlanAgent(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder
        .defaultSystem(SYSTEM_PROMPT)
        .build();
  }

  //-----------------------------------------------------------------------------------
  // 멀티 에이전트 환경에서 실행할 경우
  public void execute(PlanState state) {
    // AI에게 제공할 프롬프트 생성
    String prompt = buildTravelPlanPrompt(state);
    
    // AI가 전체 일정을 한 번에 생성
    Plan plan = chatClient.prompt()
        .user(prompt)
        .call()
        .entity(Plan.class);
    
    state.setPlan(plan);
  }
  
  //-----------------------------------------------------------------------------------
  // AI를 위한 여행 계획 프롬프트 생성
  private String buildTravelPlanPrompt(PlanState state) {
    // 관광지 목록 문자열 생성
    StringBuilder attractions = new StringBuilder();
    if (state.getAttractions() != null) {
      for (Attraction attr : state.getAttractions()) {
        attractions.append("- ").append(attr.getName())
            .append(" (입장료: ").append(String.format("%,d", attr.getEntranceFee())).append("원)\n")
            .append("  위치: ").append(attr.getAddress()).append("\n")
            .append("  설명: ").append(attr.getDescription()).append("\n");
      }
    }
    
    // 맛집 목록 문자열 생성
    StringBuilder restaurants = new StringBuilder();
    if (state.getRestaurants() != null) {
      for (Restaurant rest : state.getRestaurants()) {
        restaurants.append("- ").append(rest.getName())
            .append(" (평균 가격: ").append(String.format("%,d", rest.getPrice())).append("원)\n")
            .append("  위치: ").append(rest.getAddress()).append("\n")
            .append("  메뉴: ").append(rest.getDescription()).append("\n");
      }
    }
    
    // 숙소 목록 문자열 생성
    StringBuilder accommodations = new StringBuilder();
    if (state.getAccommodations() != null) {
      for (Accommodation acc : state.getAccommodations()) {
        accommodations.append("- ").append(acc.getName())
            .append(" (1박: ").append(String.format("%,d", acc.getPricePerNight())).append("원)\n")
            .append("  위치: ").append(acc.getAddress()).append("\n")
            .append("  특징: ").append(acc.getDescription()).append("\n");
      }
    }
    
    // 템플릿에 값 채우기
    String prompt = USER_PROMPT_TEMPLATE
        .replace("{days}", String.valueOf(state.getDays()))
        .replace("{nights}", String.valueOf(state.getDays() - 1))
        .replace("{budget}", String.format("%,d", state.getMaxBudget()))
        .replace("{attractions}", attractions.toString())
        .replace("{restaurants}", restaurants.toString())
        .replace("{accommodations}", accommodations.toString());
    
    // 재계획일 경우 프롬프트에 낮은 옵션을 선택하도록 보강
    if (state.isReplan() && state.getPreviousTotalCost() != null) {
      int exceededAmount = state.getPreviousTotalCost() - state.getMaxBudget();
      String replanWarning = String.format("""          
          **예산 재계획 필수**
          이전 일정이 예산을 %,d원 초과했습니다. (이전 총비용: %,d원)
          반드시 더 저렴한 관광지, 맛집, 숙소를 선택하여
          총 예산 %,d원 이내로 일정을 재작성해야 합니다.
          가능한 한 무료 또는 저렴한 관광지를 우선 선택하고,
          식사와 숙소도 가격이 낮은 옵션을 선택하세요.
          """,
          exceededAmount,
          state.getPreviousTotalCost(),
          state.getMaxBudget());
      prompt = prompt + replanWarning;
    }
    
    return prompt;
  }
}
