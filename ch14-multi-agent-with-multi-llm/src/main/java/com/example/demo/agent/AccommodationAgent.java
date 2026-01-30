package com.example.demo.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import com.example.demo.dto.Accommodation;
import com.example.demo.dto.PlanState;
import com.example.demo.service.InternetSearchService;

// 숙소 추천 에이전트: 인터넷 서칭하여 숙소 정보 제공
@Component
public class AccommodationAgent {
  //-----------------------------------------------------------------------------------
  // 숙소 유형별 기본 1박 요금(검색 결과에 가격이 없을 때만 사용)
  private static final Map<String, Integer> DEFAULT_PRICE_PER_NIGHT = Map.of(
      "호텔", 150000,
      "리조트", 180000,
      "펜션", 100000,
      "게스트하우스", 60000,
      "숙소", 120000
  );

  //-----------------------------------------------------------------------------------
  // 시스템 프롬프트
  private static final String SYSTEM_PROMPT = """
    당신은 숙소 추천 전문 에이전트입니다.

    ## 목표
    사용자의 요청에 맞는 숙소 후보를 여러 개 추천합니다.

    ## 사용 가능한 도구
    1) searchAccommodations: 숙소 후보 검색(요약)
    2) fetchAccommodationInfo: 후보 상세 정보 보완

    ## 규칙
    1) 숙소 후보는 최소 3개, 최대 6개를 제안하세요.
    2) pricePerNight(1박 요금)는 가능한 한 도구 결과에서 찾아 채우세요.
    3) 도구 결과에서 가격을 확인할 수 없다면 pricePerNight는 0으로 두세요(후처리에서 보정됩니다).

    ## 출력 형식
    - 반드시 JSON 배열만 출력하세요.
    - 예) [{"name":"...","address":"...","description":"...","pricePerNight":150000}]
    """;
  
  //-----------------------------------------------------------------------------------
  // 사용자 프롬프트 템플릿
  private static final String USER_PROMPT_TEMPLATE = """
    사용자 요청: %s
    - 호텔/리조트/펜션/게스트하우스 등 다양한 유형을 섞어 추천하세요.
    - 각 항목에는 name, address, description, pricePerNight를 포함하세요.
    - 반드시 JSON 배열만 출력하세요.
    """;
  
  //-----------------------------------------------------------------------------------
  private final ChatClient chatClient;
  private final InternetSearchService searchService;

  //-----------------------------------------------------------------------------------
  public AccommodationAgent(
      @Qualifier("geminiBuilder") ChatClient.Builder chatClientBuilder, 
      InternetSearchService searchService) {
    this.chatClient = chatClientBuilder
        .defaultSystem(SYSTEM_PROMPT)
        .build();
    this.searchService = searchService;
  }

  //-----------------------------------------------------------------------------------
  // 단독으로 실행할 경우
  public List<Accommodation> execute(String userQuery) {
    String userMessage = String.format(Locale.KOREAN, USER_PROMPT_TEMPLATE, userQuery);
    List<Accommodation> result = callAsEntity(userMessage);
    // LLM 출력이 완벽하지 않은 경우를 대비해 pricePerNight 보정
    return normalize(result);
  }

  //-----------------------------------------------------------------------------------
  // 멀티 에이전트 환경에서 실행할 경우
  public void execute(PlanState state) {
    // 재계획 여부에 따라 검색 쿼리를 다르게 생성
    String query;
    if (state.isReplan()) {
        query = String.format("%s 가성비 저렴한 숙소 추천", state.getDestination());
    } else {
        query = String.format("%s 숙소 추천", state.getDestination());
    }
    // 기존에 구현된 String 매개변수 버전의 execute()를 재호출 (로직 재사용)
    List<Accommodation> accommodations = execute(query);
    // 실행 결과(숙소 리스트)를 공유 객체인 state에 직접 저장
    state.setAccommodations(accommodations);
  }

  //-----------------------------------------------------------------------------------
  // LLM 호출 + 엔티티 변환(실패 시 1회 보정 재시도)
  private List<Accommodation> callAsEntity(String userMessage) {
    try {
      return chatClient.prompt()
          .user(userMessage)
          .tools(this)
          .call()
          .entity(new ParameterizedTypeReference<List<Accommodation>>() {});
    } catch (Exception first) {
      // JSON 형식이 깨지는 경우가 있어 1회 보정 재요청
      String repairMessage = """
        이전 응답이 JSON 배열 형식이 아니어서 파싱에 실패했습니다.
        반드시 JSON 배열만 다시 출력하세요. 다른 텍스트는 절대 포함하지 마세요.
        JSON 스키마: [{"name":"...","address":"...","description":"...","pricePerNight":12345}]
        """;
      return chatClient.prompt()
          .user(userMessage + "\n\n" + repairMessage)
          .tools(this)
          .call()
          .entity(new ParameterizedTypeReference<List<Accommodation>>() {});
    }
  }

  //-----------------------------------------------------------------------------------
  // pricePerNight가 0/누락이면 기본값으로 보정
  private List<Accommodation> normalize(List<Accommodation> items) {
    if (items == null || items.isEmpty()) {
      return List.of();
    }

    List<Accommodation> normalized = new ArrayList<>(items.size());
    for (Accommodation accommodation : items) {
      if (accommodation == null) {
        continue;
      }

      Integer price = accommodation.getPricePerNight();
      if (price == null || price <= 0) {
        int fallback = inferDefaultPrice(accommodation);
        // 기존 모델을 유지하기 위해 description에 출처 힌트를 간단히 남김(선택)
        String desc = accommodation.getDescription();
        String patchedDesc = (desc == null ? "" : desc);
        if (!patchedDesc.contains("기본요금")) {
          patchedDesc = patchedDesc.isBlank()
              ? String.format(Locale.KOREAN, "기본요금 적용(%d원)", fallback)
              : String.format(Locale.KOREAN, "%s (기본요금 적용: %d원)", patchedDesc, fallback);
        }

        accommodation.setPricePerNight(fallback);
        accommodation.setDescription(patchedDesc);
      }
      normalized.add(accommodation);
    }
    return normalized;
  }

  //-----------------------------------------------------------------------------------
  // 숙소 이름/설명에 포함된 키워드로 기본 가격 추정
  private int inferDefaultPrice(Accommodation a) {
    String name = safe(a.getName());
    String desc = safe(a.getDescription());
    String text = (name + " " + desc).trim();

    if (text.contains("리조트")) {
      return DEFAULT_PRICE_PER_NIGHT.get("리조트");
    }
    if (text.contains("게스트하우스") || text.contains("호스텔")) {
      return DEFAULT_PRICE_PER_NIGHT.get("게스트하우스");
    }
    if (text.contains("펜션")) {
      return DEFAULT_PRICE_PER_NIGHT.get("펜션");
    }
    if (text.contains("호텔")) {
      return DEFAULT_PRICE_PER_NIGHT.get("호텔");
    }
    return DEFAULT_PRICE_PER_NIGHT.get("숙소");
  }

  private String safe(String s) {
    return s == null ? "" : s;
  }  

  //-----------------------------------------------------------------------------------
  // Tool 1: 숙소 검색(요약)
  @Tool(description = "숙소 정보를 인터넷에서 검색합니다. 제목, 링크, 요약을 반환합니다.")
  public String searchAccommodations(@ToolParam(description = "검색 쿼리 (예: '제주도 호텔', '서울 숙소')") String query) {
    return searchService.search(query);
  }

  //-----------------------------------------------------------------------------------
  // Tool 2: 상세 페이지 본문 조회
  @Tool(description = "웹 페이지의 본문 텍스트를 가져와 숙소 상세 정보를 제공합니다.")
  public String fetchAccommodationInfo(@ToolParam(description = "웹 페이지 URL") String url) {
    return searchService.fetch(url);
  }
}
