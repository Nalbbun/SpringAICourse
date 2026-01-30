package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
  @GetMapping("/")
  public String home() {
    return "home";
  }

  @GetMapping("/image-analysis")
  public String imageAnalysis() {
    return "image-analysis";
  }

  @GetMapping("/video-analysis")
  public String videoAnalysis() {
    return "video-analysis";
  }
  
  @GetMapping("/image-generation")
  public String imageGeneration() {
    return "image-generation";
  }     
}
