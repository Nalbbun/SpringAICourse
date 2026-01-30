package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

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
  public Flux<String> chat(String question) {
    // ChatClient가 도구(Tool)를 호출할 때, 
    // MCP 서버와 통신하는 과정에서 내부적으로 block() 같은 블로킹 메서드를 사용하고 있음 
    // WebFlux는 이벤트 루프 스레드를 사용하기 때문에 이 스레드는 절대 블로킹되면 안 됨
    // 해결 방법은 블로킹 작업을 별도 스레드에서 실행하도록 해야함.

    // [해결 코드]
    // defer()는 코드 실행을 미루고 구독 시점에 실행되도록 함
    // Schedulers.boundedElastic()은 블로킹 작업 전용 스레드 풀을 반환
    // subscribeOn()은 구독시 defer()로 미뤄진 코드를 boundedElastic 스레드 풀에서 실행하도록 전달
    // Controller에서 return할 때 WebFlux가 자동으로 구독하고(subscribe() 호출) 응답 전송

    return Flux.defer(() -> {
        Flux<String> answer = this.chatClient.prompt()
            .system("""
                현재 날짜와 시간 질문은 반드시 도구를 사용하세요.
                파일과 디렉토리 관련 질문은 반드시 도구를 사용하세요.
                """)
            .user(question)
            .stream()
            .content();
        return answer;
    }).subscribeOn(Schedulers.boundedElastic());
  }

  // ##### 사진에서 차량 번호판을 인식하고 차단 봉을 제어하는 메소드 #####
  public Flux<String> boomBarrier(String contentType, byte[] bytes) {
    return Flux.defer(() -> {
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
        Flux<String> answer = chatClient.prompt()
            .messages(userMessage)
            .stream()
            .content();
        return answer;
    }).subscribeOn(Schedulers.boundedElastic());
  }
}
