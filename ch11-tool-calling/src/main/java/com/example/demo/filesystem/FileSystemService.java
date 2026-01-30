package com.example.demo.filesystem;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileSystemService {
  // ##### 필드 #####
  private ChatClient chatClient;

  @Autowired
  private FileSystemTools fileSystemTools;

  // ##### 생성자 #####
  public FileSystemService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
    this.chatClient = chatClientBuilder
      .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
      .defaultSystem("""
        파일, 디렉토리 관련 질문은 반드시 도구를 사용하세요.
      """)
      .build();
  }

  // ##### LLM과 대화하는 메소드 #####
  public String chat(String question, String conversationId) {
    String answer = this.chatClient.prompt()
        .user(question)
        .advisors(advisorSpec -> advisorSpec.param(
          ChatMemory.CONVERSATION_ID, conversationId
        ))
        .tools(fileSystemTools)
        .call()
        .content();
    return answer;
  }
}
