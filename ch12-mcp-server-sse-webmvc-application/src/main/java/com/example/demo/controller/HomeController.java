package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
  @GetMapping("/")
  public String home() {
    return "home";
  }

  @GetMapping("/chat")
  public String chat() {
    return "chat";
  }   

  @GetMapping("/boom-barrier")
  public String boomBarrier() {
    return "boom-barrier";
  }   
}
