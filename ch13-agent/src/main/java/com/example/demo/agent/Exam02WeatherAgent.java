package com.example.demo.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.example.demo.advisor.CityValidationAdvisor;

// 날씨 에이전트
@Component
public class Exam02WeatherAgent {
  //------------------------------------------------------------------------------
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
  //------------------------------------------------------------------------------
  // ChatClient 초기화하는 생성자
  // 기본 시스템 메시지 및 기본 어드바이저 설정(LLM 호출 전 흐름에 개입)
  public Exam02WeatherAgent(ChatClient.Builder builder) {
    this.chatClient = builder
        .defaultSystem(SYSTEM_PROMPT)
        .defaultAdvisors(new CityValidationAdvisor())
        .build();
  }
  //------------------------------------------------------------------------------
  // 에이전트를 실행하는 메소드
  // - 사용자 입력 전달 -> LLM 판단 수행 -> 필요 시 Tool 호출 -> 최종 응답 반환
  public String execute(String userMessage) {
    return chatClient.prompt()
        .user(userMessage)
        .tools(this)
        .call()
        .content();
  }
  //------------------------------------------------------------------------------
  // 외부 기능을 수행하는 Tool
  // - 실제 환경에서는 API 호출 코드가 위치
  @Tool(description = "특정 도시의 현재 날씨 정보를 조회합니다")
  public String getWeather(@ToolParam(description = "도시 이름") String city) {
    return String.format("%s의 현재 날씨는 맑고 23도입니다.", city);
  }

}
