package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class AiServiceRoleAssignmentPrompt {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiServiceRoleAssignmentPrompt(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  // ##### 시스템 메시지를 활용해서 역할 부여하기 #####
  public Flux<String> roleAssignment(String requirements) {
    Flux<String> travelSuggestions = chatClient.prompt()
        // 시스템 메시지 추가
        .system("""
            당신이 여행 가이드 역할을 해 주었으면 좋겠습니다.
            아래 요청사항에서 위치를 알려주면, 근처에 있는 3곳을 제안해 주고,
            이유를 달아주세요. 경우에 따라서 방문하고 싶은 장소 유형을 
            제공할 수도 있습니다.
            """)
        // 사용자 메시지 추가
        .user("요청사항: %s".formatted(requirements))
        // 대화 옵션 설정
        .options(ChatOptions.builder()
            .temperature(1.0)
            .maxTokens(1000)
            .build())
        // LLM으로 요청하고 응답 얻기
        .stream()
        .content();
    return travelSuggestions;
  }
}
