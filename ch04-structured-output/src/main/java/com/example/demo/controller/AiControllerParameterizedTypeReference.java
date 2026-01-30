package com.example.demo.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.Hotel;
import com.example.demo.service.AiServiceParameterizedTypeReference;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/ai")
@Slf4j
public class AiControllerParameterizedTypeReference {
  // ##### 필드 #####
  @Autowired
  private AiServiceParameterizedTypeReference aiService;

  //##### 메소드 #####
  @PostMapping(
    value = "/generic-bean-output-converter",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE  
  )
  public List<Hotel> genericBeanOutputConverter(@RequestParam("cities") String cities) {
    //List<Hotel> hotelList = aiService.genericBeanOutputConverterLowLevel(cities);
    List<Hotel> hotelList = aiService.genericBeanOutputConverterHighLevel(cities);
    return hotelList;
  }   
}
