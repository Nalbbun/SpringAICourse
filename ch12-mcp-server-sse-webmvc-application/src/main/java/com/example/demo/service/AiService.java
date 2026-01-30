package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiService {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiService(ChatClient.Builder chatClientBuilder, ToolCallbackProvider toolCallbackProvider) {
   this.chatClient = chatClientBuilder
      .defaultToolCallbacks(toolCallbackProvider)
      .build();
  } 
  
  // ##### LLM과 텍스트로 대화하는 메소드 #####
  public String chat(String question) {
    String answer = this.chatClient.prompt()
        .system("""
            현재 날짜와 시간 질문은 반드시 도구를 사용하세요.
            파일과 디렉토리 관련 질문은 반드시 도구를 사용하세요.
            """)
        .user(question)
        .call()
        .content();
    return answer;
  }

  // ##### 사진에서 차량 번호판을 인식하고 차단 봉을 제어하는 메소드 #####
  public String boomBarrier(String contentType, byte[] bytes) {
   // 미디어 생성
    Media media = Media.builder()
        .mimeType(MimeType.valueOf(contentType))
        .data(new ByteArrayResource(bytes))
        .build();

    // 사용자 메시지 생성
    UserMessage userMessage = UserMessage.builder()
        .text("""
            다음 단계별로 처리해 주세요.
            1단계: 이미지에서 '(숫자 2개~3개)-(한글 1자)-(숫자 4개)'로 구성된 차량 번호를 인식하세요. 예: 78라1234, 567바2558
            2단계: 인식된 차량 번호에서 끝에서부터 5번째 문자가 한글 완성형 음절이 아닐 경우에는 다시 1단계로 돌아가세요.
            3단계: 1단계에서 인식된 차량 번호가 등록된 차량 번호인지 도구로 확인을 하세요.
            4단계: 3단계의 결과가 false 라면 도구로 차단기를 내리고, true 라면 도구로 차단기를 올리세요.
            
            최종 답변은 차단기 내림 또는 차단기 올림으로 하고 추가 설명은 하지마세요.
        """)
        .media(media)
        .build();

    // LLM으로 요청하고 응답받기
    String answer = chatClient.prompt()
        .messages(userMessage)
        .call()
        .content();
    return answer;
  }
}
