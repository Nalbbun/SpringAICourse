package com.example.demo.service;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiServiceZeroShotPrompt {
  // ##### 필드 #####
  private ChatClient chatClient;
  private PromptTemplate promptTemplate = PromptTemplate.builder()
      .template("""
          영화 리뷰를 [긍정적, 중립적, 부정적] 중에서 하나로 분류하세요.
          레이블만 반환하세요.
          리뷰: {review}
        """)
      .build();

  // ##### 생성자 #####
  public AiServiceZeroShotPrompt(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder
        .defaultOptions(ChatOptions.builder()
            .temperature(0.0)
            .maxTokens(4)
            .build())
        .build();
  }

  // ##### 메소드 #####
  public String zeroShotPrompt(String review) {
    String sentiment = chatClient.prompt()
        .user(promptTemplate.render(Map.of("review", review)))
        .call()
        .content();
    return sentiment;
  }
}
