package com.example.demo.agent;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.dto.Youtube;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// YouTube 비디오 검색 에이전트
// SerpApi YouTube 검색을 사용하여 여행 장소 관련 비디오 검색 및 메타데이터 제공
// SerpApi를 통해 YouTube 데이터에 접근
@Component
public class Exam08YoutubeSearchAgent {
  //-----------------------------------------------------------------------------------
  // 시스템 프롬프트
  private static final String SYSTEM_PROMPT = """
      당신은 여행 관련 YouTube 비디오 검색 전문 에이전트입니다.
      
      ## 목표
      사용자의 요청에 맞는 YouTube 비디오를 검색하여 추천합니다.
      
      ## 사용 가능한 도구
      searchYoutubeVideos: YouTube 비디오 검색
      
      ## 출력 형식
      - 반드시 JSON 배열만 출력하세요.
      - 예) [{"title":"...","uploadDate":"2024-12-23","link":"..."}]
      - 모든 필드는 필수이며 누락하지 마세요.
      """;
  
  //-----------------------------------------------------------------------------------
  // ChatClient: LLM 요청과 응답에 사용
  private final ChatClient chatClient;

  // SerpApi 엔드포인트와 API 키
  private final String serpApiEndpoint;
  private final String serpApiKey;

  // WebClient: SerpApi 요청에 사용
  private final WebClient webClient;

  // JSON 파서로 사용
  private final ObjectMapper objectMapper = new ObjectMapper();

  //-----------------------------------------------------------------------------------
  // 생성자: ChatClient와 WebClient 초기화

  public Exam08YoutubeSearchAgent(
      ChatClient.Builder chatClientBuilder,
      @Value("${serpapi.endpoint}") String serpApiEndpoint,
      @Value("${serpapi.apiKey}") String serpApiKey,
      WebClient.Builder webClientBuilder) {
    this.serpApiEndpoint = serpApiEndpoint;
    this.serpApiKey = serpApiKey;

    // WebClient 설정: SerpApi 베이스 URL
    this.webClient = webClientBuilder
        .baseUrl(serpApiEndpoint)
        .defaultHeader("Accept", "application/json")
        .build();

    // ChatClient 초기화: 시스템 프롬프트와 도구 등록
    this.chatClient = chatClientBuilder
        .defaultSystem(SYSTEM_PROMPT)
        .build();
  }

  //-----------------------------------------------------------------------------------
  // YouTube 에이전트와 대화 - 구조화된 출력 반환
  public List<Youtube> execute(String userQuery) {
    return chatClient.prompt()
        .user(userQuery)
        .tools(this)
        .call()
        .entity(new ParameterizedTypeReference<List<Youtube>>() {});
  }

  //-----------------------------------------------------------------------------------
  // Tool 메소드: YouTube 비디오 검색
  @Tool(description = """
      YouTube에서 여행 관련 비디오를 검색합니다.
      검색 키워드를 입력하면 관련 비디오의 정보를 JSON 형식으로 제공합니다.
      각 비디오의 제목, 업로드 날짜, 링크가 포함됩니다.
      """)
  public String searchYoutubeVideos(
      @ToolParam(description = "검색 키워드 (예: '서울 여행', '부산 맛집')") String query) {

    try {
      // 검색 API 호출
      String responseBody = webClient.get()
          .uri(uriBuilder -> uriBuilder
              .queryParam("engine", "youtube")
              .queryParam("search_query", query)
              .queryParam("api_key", serpApiKey)
              .build())
          .retrieve()
          .onStatus(status -> status.value() == 403,
              response -> response.bodyToMono(String.class)
                  .map(body -> new RuntimeException("SerpApi 할당량이 초과되었습니다.")))
          .onStatus(status -> status.value() == 429,
              response -> response.bodyToMono(String.class)
                  .map(body -> new RuntimeException("SerpApi 요청 한도를 초과했습니다.")))
          .bodyToMono(String.class)
          .block();

      if (responseBody == null) {
        return String.format("'%s'에 대한 검색 결과를 가져올 수 없습니다.", query);
      }

      // JSON 파싱 및 결과 포맷팅
      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode videoResults = root.path("video_results");

      if (!videoResults.isArray() || videoResults.isEmpty()) {
        return String.format("'%s'에 대한 검색 결과가 없습니다.", query);
      }

      // 결과를 JSON 형식으로 포맷팅하여 반환
      return formatVideosAsJson(videoResults);

    } catch (Exception e) {
      return "YouTube 검색 오류: " + e.getMessage();
    }
  }

  //-----------------------------------------------------------------------------------
  // 비디오 정보를 JSON 배열 형식으로 포맷팅
  private String formatVideosAsJson(JsonNode videoResults) {
    List<String> videos = new ArrayList<>();
    
    for (JsonNode video : videoResults) {
      String title = video.path("title").asText("").replace("\"", "\\\"");
      String link = video.path("link").asText("");
      String publishedDate = video.path("published_date").asText("");

      if (!title.isEmpty() && !link.isEmpty()) {
        String videoJson = String.format(
            "{\"title\":\"%s\",\"uploadDate\":\"%s\",\"link\":\"%s\"}",
            title,
            publishedDate.isEmpty() ? "날짜 정보 없음" : publishedDate,
            link
        );
        videos.add(videoJson);
      }
    }

    return "[" + String.join(",", videos) + "]";
  }
}
