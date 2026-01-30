package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
  @GetMapping("/")
  public String home() {
    return "home";
  }
  
  @GetMapping("/list-output-converter")
  public String listOutputConverter() {
    return "list-output-converter";
  }    

  @GetMapping("/bean-output-converter")
  public String beanOutputConverter() {
    return "bean-output-converter";
  }  

  @GetMapping("/generic-bean-output-converter")
  public String genericBeanOutputConverter() {
    return "generic-bean-output-converter";
  }   

  @GetMapping("/map-output-converter")
  public String mapOutputConverter() {
    return "map-output-converter";
  }
  
  @GetMapping("/system-message")
  public String systemMessage() {
    return "system-message";
  }  
}
