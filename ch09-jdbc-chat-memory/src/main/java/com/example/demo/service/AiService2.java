package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.chatmemory.CustomChatMemoryRepositoryDialect;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiService2 {
  // ##### 필드 ##### 
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiService2(JdbcTemplate jdbcTemplate, ChatClient.Builder chatClientBuilder) {   
    JdbcChatMemoryRepository chatMemoryRepository = JdbcChatMemoryRepository.builder()
        .jdbcTemplate(jdbcTemplate)
        .dialect(new CustomChatMemoryRepositoryDialect())
        .build();
    
    ChatMemory chatMemory = MessageWindowChatMemory.builder()
        .chatMemoryRepository(chatMemoryRepository)
        .maxMessages(20)
        .build();

    this.chatClient = chatClientBuilder
        .defaultAdvisors(
            PromptChatMemoryAdvisor.builder(chatMemory).build(),
            new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE-1)
        )
        .build();
  }    

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
