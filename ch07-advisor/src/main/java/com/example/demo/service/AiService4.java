package com.example.demo.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiService4 {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiService4(ChatClient.Builder chatClientBuilder) {
    SafeGuardAdvisor safeGuardAdvisor = new SafeGuardAdvisor(
        List.of("욕설", "계좌번호", "폭력", "폭탄"),
        "해당 질문은 민감한 콘텐츠 요청이므로 응답할 수 없습니다.",
        Ordered.HIGHEST_PRECEDENCE
    );
    
    this.chatClient = chatClientBuilder
        .defaultAdvisors(safeGuardAdvisor)
        .build();
  }

  // ##### 메소드 #####
  public String advisorSafeGuard(String question) {
    String response = chatClient.prompt()
        .user(question)
        .call()
        .content();
    return response;
  } 
}
