package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

import com.example.demo.dto.ReviewClassification;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiServiceSystemMessage {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiServiceSystemMessage(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  // ##### 메소드 #####
  public ReviewClassification classifyReview(String review) {
    ReviewClassification reviewClassification = chatClient.prompt()
        .system("""
            영화 리뷰를 [POSITIVE, NEUTRAL, NEGATIVE] 중에서 하나로 분류하고,
            유효한 JSON을 반환하세요.
         """)
        .user("%s".formatted(review))
        .options(ChatOptions.builder().temperature(0.0).build())
        .call()
        .entity(ReviewClassification.class);
    return reviewClassification;
  }  
}
