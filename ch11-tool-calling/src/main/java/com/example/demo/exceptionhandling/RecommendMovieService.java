package com.example.demo.exceptionhandling;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service("recommendMovieService2")
@Slf4j
public class RecommendMovieService {
  // ##### 필드 #####
  private ChatClient chatClient;

  @Autowired
  @Qualifier("recommendMovieTools2")
  private RecommendMovieTools recommendMovieTools;

  // ##### 생성자 #####
  public RecommendMovieService(ChatModel chatModel) {
    this.chatClient = ChatClient.builder(chatModel).build();
  }

  // ##### LLM과 대화하는 메소드 #####
  public String chat(String question) {
    String answer = chatClient.prompt()
        .user("""
            질문에 대해 답변해주세요. 
            사용자 ID가 존재하지 않을 경우, 진행을 멈추고,
            '[LLM] 질문을 처리할 수 없습니다.'라고 답변을 해주세요.

            질문: %s
            """.formatted(question))  
        .tools(recommendMovieTools)
        .call()
        .content();
    return answer;
  }
}
