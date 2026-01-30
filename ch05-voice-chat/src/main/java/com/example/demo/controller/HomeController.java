package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class HomeController {
  @GetMapping("/")
  public String home() {
    return "home";
  }

  @GetMapping("/stt-tts")
  public String audioTest() {
      return "stt-tts";
  }
  
  @GetMapping("/stt-llm-tts")
  public String sttLlmTts2() {
      return "stt-llm-tts";
  }
  
  @GetMapping("/chat-voice-stt-llm-tts")
  public String chatVoiceSttLlmTts() {
      return "chat-voice-stt-llm-tts";
  }   

  @GetMapping("/chat-voice-one-model")
  public String chatVoiceOneModel() {
      return "chat-voice-one-model";
  }    
}
