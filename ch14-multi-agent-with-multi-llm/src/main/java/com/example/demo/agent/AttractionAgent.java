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

import com.example.demo.dto.Attraction;
import com.example.demo.dto.PlanState;
import com.example.demo.service.InternetSearchService;

// 관광지 추천 에이전트: 인터넷 서칭하여 관광지 정보 검색 및 입장료 제공
@Component
public class AttractionAgent {
  //-----------------------------------------------------------------------------------
  // 관광지 유형별 기본 입장료(검색 결과에 없을 때만 사용)
  private static final Map<String, Integer> DEFAULT_ENTRANCE_FEE = Map.of(
      "국립공원/자연명소", 0,
      "박물관/미술관", 5000,
      "테마파크/놀이공원", 30000,
      "사찰/문화재", 3000,
      "전망대", 15000,
      "기타 관광지", 8000
  );

  //-----------------------------------------------------------------------------------
  // 시스템 프롬프트
  private static final String SYSTEM_PROMPT = """
    당신은 관광지 추천 전문 에이전트입니다.

    ## 목표
    사용자의 요청에 맞는 관광지 후보를 여러 개 추천합니다.

    ## 사용 가능한 도구
    1) searchAttractions: 관광지 검색(요약)
    2) fetchAttractionInfo: 후보 상세 정보 보완

    ## 규칙
    1) 관광지 후보는 최소 3개, 최대 6개를 제안하세요.
    2) entranceFee(입장료)는 가능한 한 도구 결과에서 찾아 채우세요.
    3) 도구 결과에서 입장료를 확인할 수 없다면 entranceFee는 0으로 두세요(후처리에서 보정됩니다).
    4) 자연/문화/체험 등 유형이 다양하도록 후보를 섞어 제안하세요.

    ## 출력 형식
    1) 반드시 JSON 배열만 출력하세요.
    2) 예) [{"name":"...","address":"...","description":"...","entranceFee":5000}]
    """;

  //-----------------------------------------------------------------------------------
  // 사용자 프롬프트 템플릿
  private static final String USER_PROMPT_TEMPLATE = """
    사용자 요청: %s
    - 자연/문화/체험 등 다양한 유형을 섞어 관광지를 추천하세요.
    - 각 항목에는 name, address, description, entranceFee를 포함하세요.
    - 반드시 JSON 배열만 출력하세요.
    """;

  //-----------------------------------------------------------------------------------
  private final ChatClient chatClient;
  private final InternetSearchService searchService;

  //-----------------------------------------------------------------------------------
  public AttractionAgent(
      @Qualifier("ollamaBuilder") ChatClient.Builder chatClientBuilder, 
      InternetSearchService searchService) {
    this.chatClient = chatClientBuilder
        .defaultSystem(SYSTEM_PROMPT)
        .build();
    this.searchService = searchService;
  }

  //-----------------------------------------------------------------------------------
  // 단독으로 실행할 경우
  public List<Attraction> execute(String userQuery) {
    String userMessage = String.format(USER_PROMPT_TEMPLATE, userQuery);
    List<Attraction> result = callAsEntity(userMessage);
    // LLM 출력이 완벽하지 않은 경우를 대비해 entranceFee 보정
    return normalize(result);
  }

  //-----------------------------------------------------------------------------------
  // 멀티 에이전트 환경에서 실행할 경우
  public void execute(PlanState state) {
    String query;
    if (state.isReplan()) {
        query = String.format("%s 가성비 저렴한 관광지 추천", state.getDestination());
    } else {
        query = String.format("%s 관광지 추천", state.getDestination());
    }
    List<Attraction> attractions = execute(query);
    state.setAttractions(attractions);
  }

  //-----------------------------------------------------------------------------------
  // LLM 호출 + 엔티티 변환(실패 시 1회 보정 재시도)
  private List<Attraction> callAsEntity(String userMessage) {
    try {
      return chatClient.prompt()
          .user(userMessage)
          .tools(this)
          .call()
          .entity(new ParameterizedTypeReference<List<Attraction>>() {});
    } catch (Exception first) {
      String repairMessage = """
        이전 응답이 JSON 배열 형식이 아니어서 파싱에 실패했습니다.
        반드시 JSON 배열만 다시 출력하세요. 다른 텍스트는 절대 포함하지 마세요.
        JSON 스키마: [{"name":"...","address":"...","description":"...","entranceFee":12345}]
        """;
      return chatClient.prompt()
          .user(userMessage + "\n\n" + repairMessage)
          .tools(this)
          .call()
          .entity(new ParameterizedTypeReference<List<Attraction>>() {});
    }
  }

  //-----------------------------------------------------------------------------------
  // entranceFee가 0/누락이면 기본값으로 보정
  private List<Attraction> normalize(List<Attraction> items) {
    if (items == null || items.isEmpty()) {
      return List.of();
    }

    List<Attraction> normalized = new ArrayList<>(items.size());
    for (Attraction a : items) {
      if (a == null) {
        continue;
      }

      Integer fee = a.getEntranceFee();
      if (fee == null || fee <= 0) {
        int fallbackFee = inferDefaultFee(a);

        String desc = a.getDescription();
        String patchedDesc = (desc == null ? "" : desc);
        if (!patchedDesc.contains("기본요금") && !patchedDesc.contains("기본 입장료")) {
          patchedDesc = patchedDesc.isBlank()
              ? String.format(Locale.KOREAN, "기본 입장료 적용(%d원)", fallbackFee)
              : String.format(Locale.KOREAN, "%s (기본 입장료 적용: %d원)", patchedDesc, fallbackFee);
        }

        a.setEntranceFee(fallbackFee);
        a.setDescription(patchedDesc);
      }
      normalized.add(a);
    }
    return normalized;
  }

  //-----------------------------------------------------------------------------------
  // 관광지 이름/설명 키워드로 유형을 추정해 기본 입장료 선택
  private int inferDefaultFee(Attraction a) {
    String name = a.getName() == null? "" : a.getName();
    String desc = a.getDescription() == null? "" : a.getDescription();
    String text = (name + " " + desc).trim();

    if (containsAny(text, "국립공원", "공원", "자연", "폭포", "해변", "산", "트레킹", "숲길", "호수")) {
      return DEFAULT_ENTRANCE_FEE.get("국립공원/자연명소");
    }
    if (containsAny(text, "박물관", "미술관", "전시", "갤러리")) {
      return DEFAULT_ENTRANCE_FEE.get("박물관/미술관");
    }
    if (containsAny(text, "테마파크", "놀이공원", "아쿠아리움", "워터파크")) {
      return DEFAULT_ENTRANCE_FEE.get("테마파크/놀이공원");
    }
    if (containsAny(text, "사찰", "절", "문화재", "유적", "궁", "성", "한옥")) {
      return DEFAULT_ENTRANCE_FEE.get("사찰/문화재");
    }
    if (containsAny(text, "전망대", "타워", "스카이", "뷰포인트")) {
      return DEFAULT_ENTRANCE_FEE.get("전망대");
    }
    return DEFAULT_ENTRANCE_FEE.get("기타 관광지");
  }

  //-----------------------------------------------------------------------------------
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

  //-----------------------------------------------------------------------------------
  @Tool(description = "관광지 정보를 인터넷에서 검색합니다. 제목, 링크, 요약을 반환합니다.")
  public String searchAttractions(
      @ToolParam(description = "검색 쿼리 (예: '제주도 관광지', '서울 박물관')") String query) {
    return searchService.search(query);
  }

  //-----------------------------------------------------------------------------------
  @Tool(description = "웹 페이지의 본문 텍스트를 가져와 관광지 상세 정보를 제공합니다.")
  public String fetchAttractionInfo(@ToolParam(description = "웹 페이지 URL") String url) {
    return searchService.fetch(url);
  }
}
