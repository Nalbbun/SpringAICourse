package com.example.demo.controller;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.demo.agent.TravelOrchestrator;

import jakarta.servlet.http.HttpSession;

// 여행 멀티 에이전트 API 컨트롤러
@RestController
@RequestMapping("/api/ai")
public class AiController {
  // 여행 관련 멀티 에이전트 처리를 위한 오케스트레이터
  @Autowired
  private TravelOrchestrator travelOrchestrator;

  @GetMapping("/chat")
  public SseEmitter chat(@RequestParam("message") String userQuery, HttpSession session) {
    String sessionId = session.getId();

    // 실시간 이벤트 스트리밍을 위한 통로(SseEmitter) 생성
    // 데이터를 보내지 않을 경우 5분 후 자동 종료 설정
    SseEmitter emitter = new SseEmitter(300000L);

    // 비동기 스레드에서 오케스트레이터 실행
    CompletableFuture.runAsync(() -> {
      try {
        // 오케스트레이터에 emitter를 인자로 전달
        String response = travelOrchestrator.execute(userQuery, sessionId, emitter);
        // 최종 응답 전송 및 종료
        sendSseEvent(emitter, "message", response);
        sendSseEvent(emitter, "complete", "");
        emitter.complete();
      } catch (Exception e) {
        emitter.completeWithError(e);
      }

    });

    // 생성된 통로(SseEmitter)를 즉시 반환하여 연결 유지
    return emitter;
  }

  // SSE 이벤트 전송
  private void sendSseEvent(SseEmitter emitter, String event, String data) {
    try {
      emitter.send(SseEmitter.event().name(event).data(data));
    } catch (Exception e) {
      // SSE 전송 실패 시 무시
    }
  }
}
