package com.example.demo.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.service.AiService;

import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/ai")
@Slf4j
public class AiController {
  // ##### 필드 #####
  @Autowired
  private AiService aiService;

  // ##### 요청 매핑 메소드 #####
  @PostMapping(
    value = "/chat",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String chat(@RequestParam("question") String question) {
    String answer = aiService.chat(question);
    return answer;
  }  

  @PostMapping(
    value = "/boom-barrier", 
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE, 
    produces = MediaType.TEXT_PLAIN_VALUE)
  public String boomBarrier(
      @RequestParam("attach") MultipartFile attach) throws IOException {
    String answer = aiService.boomBarrier(
        attach.getContentType(), attach.getBytes());
    return answer;
  }
}
