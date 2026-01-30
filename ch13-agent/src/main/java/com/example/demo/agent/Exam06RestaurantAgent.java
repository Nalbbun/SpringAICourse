package com.example.demo.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import com.example.demo.dto.Restaurant;
import com.example.demo.service.InternetSearchService;

@Component
public class Exam06RestaurantAgent {

  // 음식 종류별 기본 가격(검색 결과에 가격이 없을 때만 사용)
  private static final Map<String, Integer> DEFAULT_PRICE = Map.of(
      "한식", 12000,
      "일식/초밥", 18000,
      "중식", 15000,
      "양식/스테이크", 25000,
      "해산물", 20000,
      "분식/간식", 8000,
      "카페/디저트", 10000,
      "기타", 15000
  );

  // 시스템 프롬프트
  private static final String SYSTEM_PROMPT = """
    당신은 맛집 추천 전문 에이전트입니다.

    ## 목표
    사용자의 요청(지역/취향/음식 종류)에 맞는 맛집 후보를 추천합니다.

    ## 사용 가능한 도구
    1) searchRestaurants: 맛집 검색(요약, 가격 정보 포함 가능)
    2) fetchRestaurantInfo: 후보 상세 정보 보완

    ## 규칙
    1) 맛집 후보는 최소 3개, 최대 5개를 제안하세요.
    2) price(1인 기준 예상 가격)는 가능한 한 도구 결과에서 찾아 채우세요.
    3) 도구 결과에서 가격을 확인할 수 없다면 price는 0으로 두세요(후처리에서 보정됩니다).
    4) 동일한 곳이 중복되지 않도록 후보를 다양하게 제안하세요.

    ## 출력 형식
    - 반드시 JSON 배열만 출력하세요.
    - 예) [{"name":"...","address":"...","description":"...","price":12000}]
    """;

  // 사용자 프롬프트 템플릿
  private static final String USER_PROMPT_TEMPLATE = """
    사용자 요청: %s
    - 지역과 음식 종류(한식/일식/중식/양식/카페 등)를 고려해 다양한 맛집을 추천하세요.
    - 각 항목에는 name, address, description, price를 포함하세요.
    - 반드시 JSON 배열만 출력하세요.
    """;

  private final ChatClient chatClient;
  private final InternetSearchService searchService;

  public Exam06RestaurantAgent(
      ChatClient.Builder chatClientBuilder,
      InternetSearchService searchService) {
    this.chatClient = chatClientBuilder
        .defaultSystem(SYSTEM_PROMPT)
        .build();
    this.searchService = searchService;
  }

  public List<Restaurant> execute(String userQuery) {
    String userMessage = String.format(USER_PROMPT_TEMPLATE, userQuery);

    List<Restaurant> result = callAsEntity(userMessage);

    // 중복 제거 + price 보정
    return normalize(deduplicateByName(result));
  }

  // LLM 호출 + 엔티티 변환(실패 시 1회 보정 재시도)
  private List<Restaurant> callAsEntity(String userMessage) {
    try {
      return chatClient.prompt()
          .user(userMessage)
          .tools(this)
          .call()
          .entity(new ParameterizedTypeReference<List<Restaurant>>() {});
    } catch (Exception first) {
      String repairMessage = """
        이전 응답이 JSON 배열 형식이 아니어서 파싱에 실패했습니다.
        반드시 JSON 배열만 다시 출력하세요. 다른 텍스트는 절대 포함하지 마세요.
        JSON 스키마: [{"name":"...","address":"...","description":"...","price":12345}]
        """;
      return chatClient.prompt()
          .user(userMessage + "\n\n" + repairMessage)
          .tools(this)
          .call()
          .entity(new ParameterizedTypeReference<List<Restaurant>>() {});
    }
  }

  // 이름 기준 중복 제거(순서 유지)
  private List<Restaurant> deduplicateByName(List<Restaurant> items) {
    if (items == null || items.isEmpty()) {
      return List.of();
    }

    Map<String, Restaurant> byName = new LinkedHashMap<>();
    for (Restaurant r : items) {
      if (r == null) {
        continue;
      }
      String name = r.getName();
      if (name == null || name.isBlank()) {
        continue;
      }
      byName.putIfAbsent(name.trim(), r);
    }
    return new ArrayList<>(byName.values());
  }

  // price가 0/누락이면 기본값으로 보정
  private List<Restaurant> normalize(List<Restaurant> items) {
    if (items == null || items.isEmpty()) {
      return List.of();
    }

    List<Restaurant> normalized = new ArrayList<>(items.size());
    for (Restaurant r : items) {
      if (r == null) {
        continue;
      }

      Integer price = r.getPrice();
      if (price == null || price <= 0) {
        int fallbackPrice = inferDefaultPrice(r);

        String desc = r.getDescription();
        String patchedDesc = (desc == null ? "" : desc);
        if (!patchedDesc.contains("기본가격") && !patchedDesc.contains("기본 가격")) {
          patchedDesc = patchedDesc.isBlank()
              ? String.format(Locale.KOREAN, "기본 가격 적용(%d원)", fallbackPrice)
              : String.format(Locale.KOREAN, "%s (기본 가격 적용: %d원)", patchedDesc, fallbackPrice);
        }

        r.setPrice(fallbackPrice);
        r.setDescription(patchedDesc);
      }
      normalized.add(r);
    }
    return normalized;
  }

  // 이름/설명 키워드로 음식 종류를 추정해 기본 가격 선택
  private int inferDefaultPrice(Restaurant r) {
    String name = r.getName() == null? "" : r.getName();
    String desc = r.getDescription() == null? "" : r.getDescription();
    String text = (name + " " + desc).trim();

    if (containsAny(text, "한식", "백반", "국밥", "국수", "칼국수", "갈비", "삼겹", "김치", "냉면", "비빔밥")) {
      return DEFAULT_PRICE.get("한식");
    }
    if (containsAny(text, "일식", "초밥", "스시", "사시미", "라멘", "돈카츠", "우동")) {
      return DEFAULT_PRICE.get("일식/초밥");
    }
    if (containsAny(text, "중식", "짜장", "짬뽕", "탕수육", "마라", "훠궈")) {
      return DEFAULT_PRICE.get("중식");
    }
    if (containsAny(text, "양식", "스테이크", "파스타", "피자", "버거", "브런치")) {
      return DEFAULT_PRICE.get("양식/스테이크");
    }
    if (containsAny(text, "해산물", "회", "조개", "대게", "랍스터", "굴", "해물")) {
      return DEFAULT_PRICE.get("해산물");
    }
    if (containsAny(text, "분식", "떡볶이", "김밥", "튀김", "순대", "라볶이")) {
      return DEFAULT_PRICE.get("분식/간식");
    }
    if (containsAny(text, "카페", "커피", "디저트", "베이커리", "케이크", "빵", "브런치 카페")) {
      return DEFAULT_PRICE.get("카페/디저트");
    }
    return DEFAULT_PRICE.get("기타");
  }

  private boolean containsAny(String text, String... keywords) {
    if (text == null || text.isBlank()) {
      return false;
    }
    for (String k : keywords) {
      if (text.contains(k)) {
        return true;
      }
    }
    return false;
  }

  @Tool(description = "맛집 정보를 인터넷에서 검색합니다. 제목, 링크, 요약을 반환합니다. 가격 정보도 함께 제공될 수 있습니다.")
  public String searchRestaurants(
      @ToolParam(description = "검색 쿼리 (예: '제주도 맛집', '서울 한식', '부산 카페')") String query) {
    return searchService.search(query);
  }

  @Tool(description = "웹 페이지의 본문 텍스트를 가져와 맛집 상세 정보를 제공합니다.")
  public String fetchRestaurantInfo(@ToolParam(description = "웹 페이지 URL") String url) {
    return searchService.fetch(url);
  }
}
