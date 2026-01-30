package com.example.demo.advisor;

import java.util.List;
import java.util.Set;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;

public class CityValidationAdvisor implements CallAdvisor {
  private static final Set<String> KOREAN_CITIES = Set.of(
      "서울", "부산", "인천", "대구", "광주", "대전", "울산", "수원", "제주"
  );

  @Override
  public String getName() { return "cityValidationAdvisor"; }

  @Override
  public int getOrder() { 
    // return Ordered.HIGHEST_PRECEDENCE;
    return Ordered.LOWEST_PRECEDENCE - 1;
  }

  @Override
  public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
    // 사용자 메시지에서 텍스트 추출
    String userText = request.prompt().getUserMessages().stream()
        .map(msg -> msg.getText())
        .reduce("", (a, b) -> a + b);
    
    // 한국 도시가 포함되어 있는지 검증
    boolean valid = KOREAN_CITIES.stream().anyMatch(city -> userText.contains(city));
    
    if (!valid) {
      // 조건 불충족 시 즉시 응답 반환
      AssistantMessage assistantMessage = new AssistantMessage("한국 도시만 날씨 정보를 지원합니다.");
      Generation generation = new Generation(assistantMessage);
      ChatResponse chatResponse = new ChatResponse(List.of(generation));
      return ChatClientResponse.builder()
          .chatResponse(chatResponse)
          .build();
    }
    
    // 검증 통과 시 다음 Advisor로 진행
    return chain.nextCall(request);
  }
}
