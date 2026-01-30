package com.example.demo.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.AiService1;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
@Slf4j
public class AIController {
  // ##### 필드 #####
  @Autowired
  private AiService1 aiService1;

  // ##### 요청 매핑 메소드 #####
  @PostMapping(
    path     = "/advisor-chain",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_NDJSON_VALUE //라인으로 구분된 청크 데이터
  )
  public Flux<String> advisorChain(@RequestBody Map<String, String> map) {
    Flux<String> response = aiService1.advisorChain2(map.get("question"));
    return response;
  }
}
