package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.AiServiceSelfConsistency;
import com.example.demo.service.AiServiceStepBackPrompt;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/ai")
@Slf4j
public class AiControllerStepBackPrompt {
  // ##### 필드 ##### 
  @Autowired
  private AiServiceStepBackPrompt aiService; 
  
  @PostMapping(
    value = "/step-back-prompt",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE  
  )
  public String stepBackPrompt(@RequestParam("question") String question) throws Exception {
    String answer = aiService.stepBackPrompt(question);
    return answer;
  }  
}
