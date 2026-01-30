package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
  @GetMapping("/")
  public String home() {
    return "home";
  }
  
  @GetMapping("/exam01-weather-agent")
  public String exam01WeatherAgent() {
    return "exam01-weather-agent";
  }
  
  @GetMapping("/exam02-weather-agent")
  public String exam02WeatherAgent() {
    return "exam02-weather-agent";
  }
  
  @GetMapping("/exam03-weather-agent")
  public String exam03WeatherAgent() {
    return "exam03-weather-agent";
  }
  
  @GetMapping("/exam04-weather-agent")
  public String exam04WeatherAgent() {
    return "exam04-weather-agent";
  }

  @GetMapping("/exam05-attraction-agent")
  public String exam06AttractionAgent() {
    return "exam05-attraction-agent";
  }
  
  @GetMapping("/exam06-restaurant-agent")
  public String exam07RestaurantAgent() {
    return "exam06-restaurant-agent";
  }  
  
  @GetMapping("/exam07-accommodation-agent")
  public String exam05AccommodationAgent() {
    return "exam07-accommodation-agent";
  }
  
  @GetMapping("/exam08-youtube-search-agent")
  public String exam08YoutubeSearchAgent() {
    return "exam08-youtube-search-agent";
  }
}
