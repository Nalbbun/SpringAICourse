package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.AiServiceFewShotPrompt;

import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/ai")
@Slf4j
public class AiControllerFewShotPrompt {
  // ##### 필드 ##### 
  @Autowired
  private AiServiceFewShotPrompt aiService; 

  // ##### 메소드 #####
  @PostMapping(
    value = "/few-shot-prompt",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
  )
  public String fewShotPrompt(@RequestParam("order") String order) {
    //서술식 주문을 JSON으로 변환
    String json = aiService.fewShotPrompt(order);
    return json;
  } 
}
