package com.example.demo.datetime;

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
public class DateTimeController {
  // ##### 필드 #####
  @Autowired
  private DateTimeService dateTimeService;
  
  // ##### 요청 매핑 메소드 #####
  @PostMapping(
    value = "/date-time-tools",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String dateTimeTools(@RequestParam("question") String question) {
    String answer = dateTimeService.chat(question);
    return answer;
  }  
}

