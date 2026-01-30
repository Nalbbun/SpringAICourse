package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.AiServiceMultiMessages;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/ai")
@Slf4j
public class AiControllerMultiMessages {
  // ##### 필드 ##### 
  @Autowired
  private AiServiceMultiMessages aiService;
  
  // ##### 요청 매핑 메소드 #####
  @PostMapping(
    value = "/multi-messages",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String multiMessages(
      @RequestParam("question") String question, HttpSession session) {
    List<Message> chatMemory = (List<Message>) session.getAttribute("chatMemory");
    if(chatMemory == null) {
      chatMemory = new ArrayList<Message>();
      session.setAttribute("chatMemory", chatMemory);
    }
    String answer = aiService.multiMessages(question, chatMemory);
    return answer;
  }
}
