package com.example.demo.agent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.demo.dto.Accommodation;
import com.example.demo.dto.Attraction;
import com.example.demo.dto.BudgetAnalysis;
import com.example.demo.dto.Plan;
import com.example.demo.dto.PlanState;
import com.example.demo.dto.Restaurant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// Travel Orchestrator - 여행 멀티 에이전트 조율자
// Tool 기반 아키텍처로 LLM이 자동으로 필요한 Agent 선택
@Component
public class TravelOrchestrator {
  // ----------------------------------------------------------------------------------------
  // ChatClient 생성을 위한 빌더
  private final ChatClient.Builder chatClientBuilder;

  // 대화의 문맥(Context)을 저장하고 관리하는 메모리
  private final ChatMemory chatMemory;

  // JSON 데이터 파싱 및 객체 변환을 담당하는 매퍼
  private final ObjectMapper objectMapper;

  // 협업을 수행할 하위 전문가 에이전트들
  private final AttractionAgent attractionAgent; // 관광지 검색 에이전트
  private final RestaurantAgent restaurantAgent; // 맛집 추천 에이전트
  private final AccommodationAgent accommodationAgent; // 숙소 정보 에이전트
  private final PlanAgent planAgent; // 일정 통합 계획 에이전트
  private final BudgetAgent budgetAgent; // 예산 검증 에이전트

  // 비동기 스레드 간 실시간 메시지(SSE) 채널 공유를 위한 저장소 
  private final InheritableThreadLocal<SseEmitter> threadLocal;

  // ----------------------------------------------------------------------------------------
  // 생성자: ChatClient와 협업할 전문가 에이전트들을 주입받아 초기화
  public TravelOrchestrator(
      ChatClient.Builder chatClientBuilder,
      ChatMemory chatMemory,
      AttractionAgent attractionAgent,
      RestaurantAgent restaurantAgent,
      AccommodationAgent accommodationAgent,
      PlanAgent planAgent,
      BudgetAgent budgetAgent) {

    this.chatClientBuilder = chatClientBuilder;
    this.chatMemory = chatMemory;

    this.attractionAgent = attractionAgent;
    this.restaurantAgent = restaurantAgent;
    this.accommodationAgent = accommodationAgent;
    this.planAgent = planAgent;
    this.budgetAgent = budgetAgent;

    this.objectMapper = new ObjectMapper();

    threadLocal = new InheritableThreadLocal<>();
  }

  // ----------------------------------------------------------------------------------------
  // Agent 실행
  public String execute(String userQuery, String conversationId, SseEmitter emitter) {
    // 해당 스레드에 SseEmitter 저장
    threadLocal.set(emitter);

    // LLM로 요청하고 응답받기
    ChatClient chatClient = chatClientBuilder.build();

    String systemMemssage = """
          당신은 여행 계획 전문 오케스트레이터(조율자)입니다.
          사용자의 질문을 분석하여 적절한 전문 에이전트에게 작업을 위임합니다.

          ## 사용 가능한 도구 (Agent):
          1. callAttractionAgent - 관광지/명소 추천
              예: "서울 명소", "부산 관광지", "제주도 가볼만한 곳"
          2. callRestaurantAgent - 맛집 추천
              예: "강남 맛집", "제주도 흑돼지", "부산 해물 맛집"
          3. callAccommodationAgent - 숙소 추천
              예: "서울 호텔", "제주도 펜션", "가성비 숙소"
          4. callMultiAgentForTravelPlan - 전체 여행 일정 생성
              예: "제주도 2박3일 100만원 예산으로 계획 짜줘"

          ## 응답 규칙:
          - 단일 정보(관광지/맛집/숙소)만 필요하면 해당 도구 하나만 호출
          - "N박N일 계획" 요청 시 callMultiAgentForTravelPlan 호출
          - 도구 호출이 필요 없는 일반 질문은 직접 답변

          당신의 주 역할은 적절한 도구(전문가 에이전트)를 호출하는 것입니다.
        """;

    String response = chatClient.prompt()
        .system(systemMemssage)
        .user(userQuery)
        .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
        .tools(this)
        .call()
        .content();

    // 현재 스레드에서 SseEmitter 해제
    threadLocal.remove();

    // 최종 답변 반환
    return response;
  }

  // ----------------------------------------------------------------------------------------
  // SSE 이벤트 전송 (내부에서 예외 처리)
  private void sendSseEvent(String agentName, String status, String message) {
    try {
      SseEmitter emitter = threadLocal.get();
      if (emitter != null) {
        emitter.send(SseEmitter.event().name("agent").data(Map.of(
            "agent", agentName,
            "status", status,
            "message", message)));
      }
    } catch (Exception e) {
      // 전송 실패 시 무시하여 주 로직의 흐름을 방해하지 않음
    }
  }

  // ----------------------------------------------------------------------------------------
  // Tool: 관광지 정보 조회
  @Tool(description = """
        관광지 정보를 조회합니다.
        사용자가 관광지, 명소, 가볼만한 곳에 대해 물어볼 때 이 도구를 사용하세요.
        예: "제주도 명소", "제주도 관광지", "제주도 가볼만한 곳"
      """, returnDirect = true)
  public List<Attraction> callAttractionAgent(String query) {
    sendSseEvent("AttractionAgent", "running", "관광지 정보 검색 중...");
    List<Attraction> attractions = attractionAgent.execute(query);
    sendSseEvent("AttractionAgent", "complete", "관광지 정보 검색 완료");
    return attractions;
  }

  // ----------------------------------------------------------------------------------------
  // Tool: 맛집 정보 조회
  @Tool(description = """
        맛집 정보를 조회합니다.
        사용자가 음식, 맛집, 레스토랑에 대해 물어볼 때 이 도구를 사용하세요.
        예: "제주도 맛집", "제주도 흑돼지 맛집", "제주도 해물 맛집"
      """, returnDirect = true)
  public List<Restaurant> callRestaurantAgent(String query) {
    sendSseEvent("RestaurantAgent", "running", "맛집 정보 검색 중...");
    List<Restaurant> restaurants = restaurantAgent.execute(query);
    sendSseEvent("RestaurantAgent", "complete", "맛집 정보 검색 완료");
    return restaurants;
  }

  // ----------------------------------------------------------------------------------------
  // Tool: 숙소 정보 조회
  @Tool(description = """
        숙소 정보를 조회합니다.
        사용자가 호텔, 숙소, 펜션, 리조트에 대해 물어볼 때 이 도구를 사용하세요.
        예: "제주도 호텔", "제주도 펜션", "제주도 가성비 숙소"
      """, returnDirect = true)
  public List<Accommodation> callAccommodationAgent(String query) {
    sendSseEvent("AccommodationAgent", "running", "숙소 정보 검색 중...");
    List<Accommodation> accommodations = accommodationAgent.execute(query);
    sendSseEvent("AccommodationAgent", "complete", "숙소 정보 검색 완료");
    return accommodations;
  }

  // ----------------------------------------------------------------------------------------
  // Tool: 여행 일정 생성
  @Tool(description = """
          전체 여행 일정을 생성합니다.
          사용자가 "N박N일 계획 짜줘", "여행 일정 만들어줘"라고 요청할 때 사용하세요.
          예: "제주도 2박3일 여행 계획 짜줘", "제주도 3박4일 여행 일정 만들어줘"
      """, returnDirect = true)
  public Plan callMultiAgentForTravelPlan(String query) {
    // 사용자 질문 파싱하여 요구사항을 state에 저장 (요구 분석 단계)
    PlanState state = parseUserQuery(query);

    // 관광지/맛집/숙소 에이전트를 병렬로 실행 (정보 수집 단게)
    collectTravelInfoInParallel(state);

    // 여행 일정을 생성하는 에이전트 실행 (여행 일정 초기 계획 단계)
    sendSseEvent("PlanAgent", "running", "초기 여행 일정 생성 중...");
    planAgent.execute(state);
    sendSseEvent("PlanAgent", "complete", "초기 여행 일정 생성 완료");

    // 예산을 분석하는 에이전트 실행 (검증 단계)
    sendSseEvent("BudgetAgent", "running", "예산 분석 중...");
    budgetAgent.execute(state);
    sendSseEvent("BudgetAgent", "complete", state.getBudgetAnalysis().getMessage());

    // 예산 초과 시에만 재계획 수행 (피드백 루프)
    if (state.getBudgetAnalysis().isExceeded()) {
      sendSseEvent("PlanAgent", "warning", "예산 초과 - 재계획 필요");
      replanWithAdjustedBudget(state);
    }
    // 최종 plan 반환
    return state.getPlan();
  }

  // ----------------------------------------------------------------------------------------
  // 사용자 질문 파싱 메서드
  private PlanState parseUserQuery(String userQuery) {
    PlanState state = new PlanState();
    state.setUserQuery(userQuery);

    // LLM을 사용하여 구조화된 정보 추출
    String userMessage = String.format("""
        다음 사용자 질문에서 여행 정보를 추출하여 JSON 형식으로 반환하세요.

        사용자 질문: "%s"

        추출할 정보:
        - destination: 여행지, 예) 제주도
        - days: 여행 전체 일수, 예) 3, 4
        - maxBudget: 예산 (숫자만, 원 단위), 예) 200000, 1000000

        추출 규칙:
        - destination은 "제주도"로 고정값
        - days는 "N박N일"에서 마지막 숫자 추출 (예: "2박3일" → 3)
        - maxBudget은 원 단위 숫자로 변환:
          * "20만원" → 200000 (20 곱하기 10000)
          * "100만원" → 1000000 (100 곱하기 10000)
          * "50만원" → 500000 (50 곱하기 10000)
          * "200만원" → 2000000 (200 곱하기 10000)

        예시 변환:
        - "제주도 2박3일 20만원" → {"destination": "제주도", "days": 3, "maxBudget": 200000}
        - "제주도 3박4일 100만원" → {"destination": "제주도", "days": 4, "maxBudget": 1000000}
        - "제주도 1박2일 50만원" → {"destination": "제주도", "days": 2, "maxBudget": 500000}

        **중요**: "X만원"에서 X는 만원 단위 숫자입니다. "20만원"은 20×10000=200000원입니다.

        JSON만 반환하고 다른 설명은 추가하지 마세요.
        """, userQuery);

    ChatClient chatClient = chatClientBuilder.build();
    String jsonResponse = chatClient.prompt()
        .user(userMessage)
        .call()
        .content();

    try {
      // 마크다운 코드 블록 제거 (```json ... ``` 또는 ``` ... ```)
      String cleanJson = jsonResponse.trim();
      if (cleanJson.startsWith("```")) {
        cleanJson = cleanJson.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("```\\s*$", "").trim();
      }

      // JSON 파싱 (재사용 가능한 ObjectMapper 사용)
      JsonNode jsonNode = objectMapper.readTree(cleanJson);

      if (jsonNode.has("destination") && !jsonNode.get("destination").isNull()) {
        state.setDestination(jsonNode.get("destination").asText());
      }
      if (jsonNode.has("days") && !jsonNode.get("days").isNull()) {
        state.setDays(jsonNode.get("days").asInt());
      }
      if (jsonNode.has("maxBudget") && !jsonNode.get("maxBudget").isNull()) {
        state.setMaxBudget(jsonNode.get("maxBudget").asInt());
      }
    } catch (Exception e) {
      // 파싱 실패 시 기본값 반환
    }
    // BudgetAnalysis 초기화 (NPE 방지)
    state.setBudgetAnalysis(new BudgetAnalysis(
        state.getMaxBudget(), 0, false, "예산 분석 대기 중"));

    return state;
  }

  // ----------------------------------------------------------------------------------------
  // 병렬로 여행 정보 수집 (관광지/맛집/숙소/날씨)
  private void collectTravelInfoInParallel(PlanState state) {
    // 현재 스레드의 SseEmitter 얻기
    final SseEmitter emitter = threadLocal.get();

    // 관광지 검색
    CompletableFuture<Void> attractionTask = CompletableFuture.runAsync(() -> {
      // CompletableFuture.runAsync()는 ForkJoinPool의 워커 스레드를 사용
      // ForkJoinPool은 InheritableThreadLocal 상속을 지원하지 않으므로
      // 스레드풀의 스레드에 명시적으로 SseEmitter를 저장해야함
      threadLocal.set(emitter);
      sendSseEvent("AttractionAgent", "running", "관광지 검색 중...");
      attractionAgent.execute(state);
      sendSseEvent("AttractionAgent", "complete", "관광지 검색 완료");
    });

    // 맛집 검색
    CompletableFuture<Void> restaurantTask = CompletableFuture.runAsync(() -> {
      threadLocal.set(emitter);
      sendSseEvent("RestaurantAgent", "running", "맛집 검색 중...");
      restaurantAgent.execute(state);
      sendSseEvent("RestaurantAgent", "complete", "맛집 검색 완료");
    });

    // 숙소 검색
    CompletableFuture<Void> accommodationTask = CompletableFuture.runAsync(() -> {
      threadLocal.set(emitter);
      sendSseEvent("AccommodationAgent", "running", "숙소 검색 중...");
      accommodationAgent.execute(state);
      sendSseEvent("AccommodationAgent", "complete", "숙소 검색 완료");
    });

    // 모든 병렬 작업 완료 대기
    CompletableFuture.allOf(attractionTask, restaurantTask, accommodationTask).join();
  }

  // ----------------------------------------------------------------------------------------
  // 예산 초과 시 여행 계획 재작성
  private void replanWithAdjustedBudget(PlanState state) {
    sendSseEvent("PlanAgent", "warning", "더 저렴한 옵션으로 재계획 중...");

    // 재계획 정보 설정
    state.setReplan(true);
    state.setPreviousTotalCost(state.getPlan().getTotalCost());

    final SseEmitter emitter = threadLocal.get();

    // 저렴한 옵션으로 관광지 재검색
    CompletableFuture<Void> attractionReSearch = CompletableFuture.runAsync(() -> {
      threadLocal.set(emitter);
      try {
        sendSseEvent("AttractionAgent", "running", "관광지 재검색 중...");
        attractionAgent.execute(state);
        sendSseEvent("AttractionAgent", "complete", "관광지 재검색 완료");
      } catch (Exception e) {
        // 예외 발생 시 무시
      }
    });

    CompletableFuture<Void> accommodationReSearch = CompletableFuture.runAsync(() -> {
      threadLocal.set(emitter);
      try {
        sendSseEvent("AccommodationAgent", "running", "숙소 재검색 중...");
        accommodationAgent.execute(state);
        sendSseEvent("AccommodationAgent", "complete", "숙소 재검색 완료");
      } catch (Exception e) {
        // 예외 발생 시 무시
      }
    });

    CompletableFuture<Void> restaurantReSearch = CompletableFuture.runAsync(() -> {
      threadLocal.set(emitter);
      try {
        sendSseEvent("RestaurantAgent", "running", "맛집 재검색 중...");
        restaurantAgent.execute(state);
        sendSseEvent("RestaurantAgent", "complete", "맛집 재검색 완료");
      } catch (Exception e) {
        // 예외 발생 시 무시
      }
    });

    // 병렬 처리가 모두 완료될 때까지 블로킹
    CompletableFuture.allOf(attractionReSearch, restaurantReSearch, accommodationReSearch).join();

    // 여행 일정 재계획
    sendSseEvent("PlanAgent", "running", "일정 재계획 중...");
    planAgent.execute(state);
    sendSseEvent("PlanAgent", "complete", "일정 재계획 완료");

    // 예산 재검증
    sendSseEvent("BudgetAgent", "running", "일정 재계획 예산 검증");
    budgetAgent.execute(state);
    sendSseEvent("BudgetAgent", "complete", state.getBudgetAnalysis().getMessage());
  }
}