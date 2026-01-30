package com.example.demo.datetime;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DateTimeService {
  // ##### 필드 #####
  private ChatClient chatClient;

  @Autowired
  private DateTimeTools dateTimeTools;

  // ##### 생성자 #####
  public DateTimeService(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder
        .build();
  }

  // ##### LLM과 대화하는 메소드 #####
  public String chat(String question) {
    String answer = this.chatClient.prompt()
        .user(question)
        .tools(dateTimeTools)
        .call()
        .content();
    return answer;
  }
}
