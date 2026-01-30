package com.example.demo.agent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.example.demo.advisor.CityValidationAdvisor;

@Component
public class Exam04WeatherAgent {

  private static final String SYSTEM_PROMPT = """
    당신은 날씨 정보를 제공하는 전문 에이전트입니다.
    날씨 정보가 필요하면 반드시 Tool을 사용해 조회하세요.
    추측으로 답변하지 마세요.

    ## 사용 가능한 도구
    1. getCurrentWeather: 특정 도시의 현재 날씨 조회
    2. getWeeklyForecast: 특정 도시의 주간 예보 조회
    3. getCurrentDate: 오늘 날짜 조회

    ## 규칙
    1. 사용자가 '주간', '이번 주', '예보', '5일'을 언급하면 getWeeklyForecast를 사용하세요.
    2. 사용자가 '오늘', '지금', '현재'를 언급하면 getCurrentWeather를 사용하세요.
    3. 그 외의 경우에는 질문의 의도에 맞게 적절한 Tool을 선택하거나, 예보가 불가능한 시점에 대해서는 일반적인 기후 특성을 안내하세요.
    4. 사용자가 도시를 명시하지 않고 후속 질문(예: "그럼 내일은?", "이번 주는?")을 하면, 이전 대화에서 마지막으로 언급된 도시를 기준으로 답변하세요.
  """;

  private final ChatClient chatClient;

  public Exam04WeatherAgent(ChatClient.Builder builder, ChatMemory chatMemory) {
    this.chatClient = builder
        .defaultSystem(SYSTEM_PROMPT)
        .defaultAdvisors(new CityValidationAdvisor())
        // 대화 맥락 유지를 위한 Advisor
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        .build();
  }

  // conversationId를 통해 세션별로 대화 맥락 유지
  public String execute(String conversationId, String userQuery) {
    return chatClient.prompt()
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
        .user(userQuery)
        .tools(this)
        .call()
        .content();
  }

  @Tool(description = "오늘 날짜를 조회합니다")
  public String getCurrentDate() {
    LocalDate now = LocalDate.now();
    return now.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 E요일", Locale.KOREAN));
  }

  @Tool(description = "특정 도시의 현재 날씨 정보를 조회합니다")
  public String getCurrentWeather(@ToolParam(description = "도시 이름") String city) {
    return String.format("%s의 현재 날씨는 맑고 23도입니다.", city);
  }

  @Tool(description = "특정 도시의 주간(5일) 날씨 예보를 조회합니다")
  public String getWeeklyForecast(@ToolParam(description = "도시 이름") String city) {
    return String.format("""
      [%s 5일 예보]
      - 월: 맑음 22°C
      - 화: 흐림 20°C
      - 수: 비 18°C
      - 목: 맑음 21°C
      - 금: 맑음 23°C
      """, city);
  }
}

// 외부 API 연동 /////////////////////////////////////////////////////////////
/*
package com.example.demo.agent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.advisor.CityValidationAdvisor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

// 날씨 정보를 제공하는 에이전트
// OpenWeatherMap API를 사용하여 현재 날씨와 5일 예보를 조회
@Component
@Slf4j
public class Exam04WeatherAgent {
  private static final String SYSTEM_PROMPT = """
    당신은 날씨 정보를 제공하는 전문 에이전트입니다.
    반드시 실제 날씨 도구를 사용하여 정확한 날씨 정보를 조회하세요.

    ## 사용 가능한 도구:
    1. getCurrentDateTime: 현재 날짜와 시간 조회
    2. getWeatherInfo: 특정 날짜의 날씨 조회
    3. getWeeklyForecast: 주간 날씨 예보 조회

    ## 규칙
    1. 사용자가 '주간', '이번 주', '예보', '5일'을 언급하면 getWeeklyForecast를 사용하세요.
    2. 사용자가 '오늘', '지금', '현재'를 언급하면 getCurrentWeather를 사용하세요.
    3. 그 외의 경우에는 질문의 의도에 맞게 적절한 Tool을 선택하거나, 예보가 불가능한 시점에 대해서는 일반적인 기후 특성을 안내하세요.
    4. 사용자가 도시를 명시하지 않고 후속 질문(예: "그럼 내일은?", "이번 주는?")을 하면, 이전 대화에서 마지막으로 언급된 도시를 기준으로 답변하세요.

    ## 제공할 정보
    - 평균 기온 (최저/최고)
    - 강수 확률
    - 날씨 상태 (맑음, 흐림, 비, 눈 등)
  """; 

  private final ChatClient chatClient;
  private final String apiKey;
  private final WebClient webClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  // 생성자: ChatClient와 WebClient 초기화
  public Exam04WeatherAgent(
      ChatClient.Builder chatClientBuilder, ChatMemory chatMemory,
      @Value("${openweather.api.key}") String apiKey,
      WebClient.Builder webClientBuilder) {

    this.apiKey = apiKey;

    // WebClient 설정: OpenWeatherMap API 베이스 URL
    this.webClient = webClientBuilder
        .baseUrl("https://api.openweathermap.org/data/2.5")
        .build();

    // ChatClient 초기화: 시스템 프롬프트와 도구 등록
    this.chatClient = chatClientBuilder
        .defaultSystem(SYSTEM_PROMPT)
        .defaultAdvisors(new CityValidationAdvisor())
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        .build();
  }

  // 사용자 질문에 대한 응답 생성
  public String execute(String conversationId, String userQuery) {
    String result = chatClient.prompt()
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
        .user(userQuery)
        .tools(this)
        .call()
        .content();
    return result;
  }

  // Tool 1: 현재 날짜와 시간 조회
  @Tool(description = "현재 날짜와 시간을 조회합니다. 사용자가 특정 월이나 시기에 대해 물을 때 먼저 이 도구를 사용하여 현재 날짜를 확인하세요.")
  public String getCurrentDateTime() {
    LocalDate now = LocalDate.now();
    String formattedDate = now.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 E요일", Locale.KOREAN));
    return formattedDate;
  }

  // Tool 2: 현재 날씨 조회
  // OpenWeatherMap Current Weather API 사용
  @Tool(description = "특정 도시의 현재 날씨 정보를 조회합니다")
  public String getWeatherInfo(
      @ToolParam(description = "도시 이름 (영문)") String city,
      @ToolParam(description = "날짜 (yyyy-MM-dd)") String date) {

    try {
      // OpenWeatherMap Current Weather API 호출
      String response = webClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/weather")
              .queryParam("q", city)
              .queryParam("appid", apiKey)
              .queryParam("units", "metric")
              .queryParam("lang", "kr")
              .build())
          .retrieve()
          .bodyToMono(String.class)
          .block();

      if (response == null) {
        return String.format("[날씨 정보를 가져올 수 없습니다: %s]", city);
      }

      // JSON 파싱
      JsonNode root = objectMapper.readTree(response);
      JsonNode main = root.get("main");
      JsonNode weather = root.get("weather").get(0);

      double temp = main.get("temp").asDouble();
      int humidity = main.get("humidity").asInt();
      String description = weather.get("description").asText(); // 한글 설명
      String weatherMain = weather.get("main").asText(); // 영문 상태

      String weatherInfo = String.format("""
          [날씨 정보]
          도시: %s
          날짜: %s
          날씨: %s (%s)
          온도: %.1f°C
          습도: %d%%
          체감온도: %.1f°C
          최저/최고: %.1f°C / %.1f°C
          """, city, date, description, weatherMain, temp, humidity,
          main.get("feels_like").asDouble(),
          main.get("temp_min").asDouble(),
          main.get("temp_max").asDouble());
      return weatherInfo;
    } catch (Exception e) {
      return String.format("[날씨 정보 조회 실패: %s - %s]", city, e.getMessage());
    }
  }

  // Tool 3: 5일 날씨 예보 조회
  // OpenWeatherMap 5 Day Forecast API 사용 (3시간 간격)
  @Tool(description = "특정 도시의 5일 날씨 예보를 조회합니다 (3시간 간격)")
  public String getWeeklyForecast(
      @ToolParam(description = "도시 이름 (영문)") String city,
      @ToolParam(description = "시작 날짜 (yyyy-MM-dd)") String startDate,
      @ToolParam(description = "일수 (최대 5일)") int days) {

    try {
      // OpenWeatherMap 5 Day Forecast API 호출
      String response = webClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/forecast")
              .queryParam("q", city)
              .queryParam("appid", apiKey)
              .queryParam("units", "metric")
              .queryParam("lang", "kr")
              .build())
          .retrieve()
          .bodyToMono(String.class)
          .block();

      if (response == null) {
        return String.format("[날씨 예보를 가져올 수 없습니다: %s]", city);
      }

      // JSON 파싱
      JsonNode root = objectMapper.readTree(response);
      JsonNode list = root.get("list");

      StringBuilder forecast = new StringBuilder();
      forecast.append(String.format("[%s 5일 날씨 예보]\n\n", city));

      String currentDate = "";
      double dayTempMax = -100;
      double dayTempMin = 100;
      StringBuilder dayWeather = new StringBuilder();

      // 각 예보 데이터 처리 (3시간 간격)
      for (JsonNode item : list) {
        // 날짜/시간 분리 (예: "2024-03-15 12:00:00" → "2024-03-15", "12:00:00")
        String dateTime = item.get("dt_txt").asText();
        String date = dateTime.split(" ")[0];
        String time = dateTime.split(" ")[1];

        // 날짜가 바뀌면 이전 날짜 요약 출력
        if (!date.equals(currentDate)) {
          if (!currentDate.isEmpty()) {
            // 이전 날짜 요약 (최고/최저 온도 + 시간대별 상세)
            forecast.append(String.format("%s: 최고 %.1f°C / 최저 %.1f°C\n%s\n",
                LocalDate.parse(currentDate).format(DateTimeFormatter.ofPattern("MM/dd(E)")),
                dayTempMax, dayTempMin, dayWeather.toString()));
          }
          // 새로운 날짜 시작 - 변수 초기화
          currentDate = date;
          dayTempMax = -100;
          dayTempMin = 100;
          dayWeather = new StringBuilder();
        }

        double temp = item.get("main").get("temp").asDouble();
        String description = item.get("weather").get(0).get("description").asText();

        dayTempMax = Math.max(dayTempMax, temp);
        dayTempMin = Math.min(dayTempMin, temp);
        dayWeather.append(String.format("  %s: %.1f°C, %s\n", time, temp, description));
      }

      // 마지막 날 추가
      if (!currentDate.isEmpty()) {
        forecast.append(String.format("%s: 최고 %.1f°C / 최저 %.1f°C\n%s",
            LocalDate.parse(currentDate).format(DateTimeFormatter.ofPattern("MM/dd(E)")),
            dayTempMax, dayTempMin, dayWeather.toString()));
      }

      return forecast.toString();

    } catch (Exception e) {
      return String.format("[날씨 예보 조회 실패: %s - %s]", city, e.getMessage());
    }
  }
}
*/