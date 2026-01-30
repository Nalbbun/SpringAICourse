package com.example.demo.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiServiceListOutputConverter {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiServiceListOutputConverter(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  // ##### 메소드 #####
  public List<String> listOutputConverterLowLevel(String city) {
    // 구조화된 출력 변환기 생성
    ListOutputConverter converter = new ListOutputConverter();
    // 프롬프트 템플릿 생성
    PromptTemplate promptTemplate = PromptTemplate.builder()
        .template("{city}에서 유명한 호텔 목록 5개를 출력하세요. {format}")
        .build();
    // 프롬프트 생성
    Prompt prompt = promptTemplate.create(
        Map.of("city", city, "format", converter.getFormat()));
    // LLM의 쉼표로 구분된 텍스트 출력 얻기
    String commaSeparatedString = chatClient.prompt(prompt)
        .call()
        .content();
    // List<String>으로 변환
    List<String> hotelList = converter.convert(commaSeparatedString);
    return hotelList;
  }
  
  public List<String> listOutputConverterHighLevel(String city) {
    List<String> hotelList = chatClient.prompt()
        .user("%s에서 유명한 호텔 목록 5개를 출력하세요.".formatted(city))
        .call()
        .entity(new ListOutputConverter());
    return hotelList;
  }
}
