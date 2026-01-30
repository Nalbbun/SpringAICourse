package com.example.demo.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

// 날씨 에이전트
@Component
public class Exam01WeatherAgent {
  // 에이전트의 역할과 행동 범위를 정의하는 시스템 프롬프트 필드
  private static final String SYSTEM_PROMPT = """
    당신은 날씨 정보를 제공하는 전문 에이전트입니다.
    날씨 정보가 필요하면 반드시 Tool을 사용해 조회하세요.
    추측으로 답변하지 마세요.

    ## 사용 가능한 Tool
    1. getWeather: 특정 도시의 현재 날씨 정보를 조회
  """;

  // ChatClient 필드
  private final ChatClient chatClient;

  // 시스템 프롬프트와 함께 ChatClient 초기화하는 생성자
  public Exam01WeatherAgent(ChatClient.Builder builder) {
    this.chatClient = builder
        .defaultSystem(SYSTEM_PROMPT)
        .build();
  }

  // 에이전트를 실행하는 메서드
  public String execute(String userQuery) {
    return chatClient.prompt()
        .user(userQuery)
        .tools(this)
        .call()
        .content();
  }

  // 외부 기능을 수행하는 Tool
  @Tool(description = "특정 도시의 현재 날씨 정보를 조회합니다")
  public String getWeather(@ToolParam(description = "도시 이름") String city) {
    // 실제 환경에서는 API 호출 코드가 위치
    return String.format("%s의 현재 날씨는 맑고 23도입니다.", city);
  }
}

