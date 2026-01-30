package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.AiService1;
import com.example.demo.service.AiService2;
import com.example.demo.service.AiService3;
import com.example.demo.service.AiService4;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/ai")
@Slf4j
public class AIController {
  // ##### 필드 #####
  @Autowired
  private AiService1 aiService1;
  @Autowired
  private AiService2 aiService2;
  @Autowired
  private AiService3 aiService3;  
  @Autowired
  private AiService4 aiService4;   

  // ##### 요청 매핑 메소드 #####
  @PostMapping(
      value = "/advisor-chain", 
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, 
      produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String advisorChain(@RequestParam("question") String question) {
    String response = aiService1.advisorChain1(question);
    return response;
  }
  
  @PostMapping(
      value = "/advisor-context", 
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, 
      produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String advisorContext(@RequestParam("question") String question) {
    String response = aiService2.advisorContext(question);
    return response;
  }  
  
  @PostMapping(
      value = "/advisor-logging", 
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, 
      produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String advisorLogging(@RequestParam("question") String question) {
    String response = aiService3.advisorLogging(question);
    return response;
  }   
  
  @PostMapping(
      value = "/advisor-safe-guard", 
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, 
      produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String advisorSafeGuard(@RequestParam("question") String question) {
    String response = aiService4.advisorSafeGuard(question);
    return response;
  }   
}
