package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

import com.example.demo.advisor.MaxCharLengthAdvisor;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiService3 {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiService3(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder
        .defaultAdvisors(
            new MaxCharLengthAdvisor(Ordered.HIGHEST_PRECEDENCE),
            new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE-1)
        )
        .build();
  }

  // ##### 메소드 #####
  public String advisorLogging(String question) {
    String response = chatClient.prompt()
        .advisors(advisorSpec -> advisorSpec.param("maxCharLength", 100))
        .user(question)
        .call()
        .content();
    return response;
  } 
}
