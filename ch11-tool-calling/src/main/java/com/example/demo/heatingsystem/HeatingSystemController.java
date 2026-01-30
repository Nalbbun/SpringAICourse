package com.example.demo.heatingsystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/ai")
@Slf4j
public class HeatingSystemController {
  // ##### 필드 #####
  @Autowired
  private HeatingSystemService heatingSystemService;
  
  // ##### 요청 매핑 메소드 #####
  @PostMapping(
    value = "/heating-system-tools",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String heatingSystemTools(@RequestParam("question") String question) {
    String answer = heatingSystemService.chat(question);
    return answer;
  }  
}

