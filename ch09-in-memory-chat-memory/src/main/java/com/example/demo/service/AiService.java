package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiService {
  // ##### 필드 ##### 
  private ChatClient chatClient;
  
  // ##### 생성자 #####
  public AiService(
      ChatMemory chatMemory, 
      ChatClient.Builder chatClientBuilder) {   
      this.chatClient = chatClientBuilder
          .defaultAdvisors(
              MessageChatMemoryAdvisor.builder(chatMemory).build(),
              //PromptChatMemoryAdvisor.builder(chatMemory).build(),
              new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE-1)
          )
          .build();
  }
  
  
  // ##### 메소드 #####
  public String chat(String userText, String conversationId) {
    String answer = chatClient.prompt()
      .user(userText)
      .advisors(advisorSpec -> advisorSpec.param(
        ChatMemory.CONVERSATION_ID, conversationId
      ))
      .call()
      .content();
    return answer;
  }
}
