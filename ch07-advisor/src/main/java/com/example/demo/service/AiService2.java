package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

import com.example.demo.advisor.MaxCharLengthAdvisor;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiService2 {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiService2(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder
        .defaultAdvisors(new MaxCharLengthAdvisor(Ordered.HIGHEST_PRECEDENCE))
        .build();
  }

  // ##### 메소드 #####
  public String advisorContext(String question) {
    String response = chatClient.prompt()
        .advisors(advisorSpec -> 
          advisorSpec.param(MaxCharLengthAdvisor.MAX_CHAR_LENGTH, 100))
        .user(question)
        .call()
        .content();
    return response;
  } 
}
