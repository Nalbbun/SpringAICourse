package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.agent.Exam01WeatherAgent;
import com.example.demo.agent.Exam02WeatherAgent;
import com.example.demo.agent.Exam03WeatherAgent;
import com.example.demo.agent.Exam04WeatherAgent;
import com.example.demo.agent.Exam07AccommodationAgent;
import com.example.demo.agent.Exam05AttractionAgent;
import com.example.demo.agent.Exam06RestaurantAgent;
import com.example.demo.agent.Exam08YoutubeSearchAgent;
import com.example.demo.dto.Accommodation;
import com.example.demo.dto.Attraction;
import com.example.demo.dto.Restaurant;
import com.example.demo.dto.Youtube;

@RestController
@RequestMapping("/ai")
public class AiController {
  // ##### 필드 #####
  @Autowired
  private Exam01WeatherAgent exam01WeatherAgent;
  
  @Autowired
  private Exam02WeatherAgent exam02WeatherAgent;
  
  @Autowired
  private Exam03WeatherAgent exam03WeatherAgent;
  
  @Autowired
  private Exam04WeatherAgent exam04WeatherAgent;
  
  @Autowired
  private Exam07AccommodationAgent exam07AccommodationAgent;
  
  @Autowired
  private Exam05AttractionAgent exam05AttractionAgent;
  
  @Autowired
  private Exam06RestaurantAgent exam06RestaurantAgent;
  
  @Autowired
  private Exam08YoutubeSearchAgent exam08YoutubeSearchAgent;

  // ##### 요청 매핑 메소드 #####
  @PostMapping(
    value = "/exam01-weather-agent",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String exam01WeatherAgent(@RequestParam("question") String question) {
    return exam01WeatherAgent.execute(question);
  }
  
  @PostMapping(
    value = "/exam02-weather-agent",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String exam02WeatherAgent(@RequestParam("question") String question) {
    return exam02WeatherAgent.execute(question);
  }
  
  @PostMapping(
    value = "/exam03-weather-agent",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String exam03WeatherAgent(@RequestParam("question") String question) {
    return exam03WeatherAgent.execute(question);
  }
  
  @PostMapping(
    value = "/exam04-weather-agent",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String exam04WeatherAgent(
      @RequestParam("conversationId") String conversationId,
      @RequestParam("question") String question) {
    return exam04WeatherAgent.execute(conversationId, question);
  }

  @PostMapping(
    value = "/exam05-attraction-agent",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
  )
  public List<Attraction> exam06AttractionAgent(@RequestParam("question") String question) {
    return exam05AttractionAgent.execute(question);
  }
  
  @PostMapping(
    value = "/exam06-restaurant-agent",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
  )
  public List<Restaurant> exam07RestaurantAgent(@RequestParam("question") String question) {
    return exam06RestaurantAgent.execute(question);
  }  
  
  @PostMapping(
    value = "/exam07-accommodation-agent",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
  )
  public List<Accommodation> exam05AccommodationAgent(@RequestParam("question") String question) {
    return exam07AccommodationAgent.execute(question);
  }
  
  @PostMapping(
    value = "/exam08-youtube-search",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
  )
  public List<Youtube> exam08YoutubeSearch(@RequestParam("question") String question) {
    return exam08YoutubeSearchAgent.execute(question);
  }
}
