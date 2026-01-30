package com.example.demo.service;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiServiceSelfConsistency {
  // ##### 필드 #####
  private ChatClient chatClient;
  private PromptTemplate promptTemplate = PromptTemplate.builder()
      .template("""
          다음 내용을 IMPORTANT, NOT_IMPORTANT 둘 중 하나로 분류해 주세요.
          레이블만 반환하세요.
          내용: {content}
          """)
      .build();

  // ##### 생성자 #####
  public AiServiceSelfConsistency(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  // ##### 메소드 #####
  public String selfConsistency(String content) {
    int importantCount = 0;
    int notImportantCount = 0;
    
    String userText = promptTemplate.render(Map.of("content", content));
  
    // 다섯번에 걸쳐 응답 받아 보기
    for (int i = 0; i < 5; i++) {
      // LLM 요청 및 응답 받기
      String output = chatClient.prompt()
          .user(userText)
          .options(ChatOptions.builder()
              .temperature(1.0)
              .build())
          .call()
          .content();
  
      log.info("{}: {}", i, output.toString());
  
      // 결과 집계
      if (output.equals("IMPORTANT")) {
        importantCount++;
      } else {
        notImportantCount++;
      }
    }
  
    // 다수결로 최종 분류를 결정
    String finalClassification = importantCount > notImportantCount ?
            "중요함" : "중요하지 않음";
    return finalClassification;
  }
}
