package com.example.demo.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiServiceMultiMessages {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiServiceMultiMessages(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  // ##### 메소드 #####
  public String multiMessages(String question, List<Message> chatMemory) {
    // 시스템 메시지 생성
    SystemMessage systemMessage = SystemMessage.builder()
        .text("""
            당신은 AI 비서입니다.
            제공되는 지난 대화 내용을 보고 우선적으로 답변해주세요.
        """)
        .build();
    
    // 대화를 처음 시작할 경우, 시스템 메시지 저장
    if(chatMemory.size() == 0) {
      chatMemory.add(systemMessage);
    }
    
    // 이전 대화 내용 출력
    log.info(chatMemory.toString());
    
    // LLM에게 요청하고 응답받기
    ChatResponse chatResponse = chatClient.prompt()
        // 이전 대화 내용 추가
        .messages(chatMemory)
        // 사용자 메시지 추가
        .user(question)
        // 동기 방식으로 답변 얻기
        .call()
        // ChatResponse로 반환하기
        .chatResponse();
  
    // 대화 메시지 저장
    UserMessage userMessage = UserMessage.builder().text(question).build();
    chatMemory.add(userMessage);
    
    AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
    chatMemory.add(assistantMessage);
  
    // LLM의 텍스트 답변 반환
    String text = assistantMessage.getText();
    return text;
  }
}
