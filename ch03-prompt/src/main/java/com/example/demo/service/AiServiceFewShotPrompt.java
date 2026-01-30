package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiServiceFewShotPrompt {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiServiceFewShotPrompt(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  // ##### 메소드 #####
  public String fewShotPrompt(String order) {
    // 프롬프트 생성
    String strPrompt = """
        고객 주문을 유효한 JSON 형식으로 바꿔주세요.
        추가 설명은 포함하지 마세요.

        예시1:
        작은 피자 하나, 치즈랑 토마토 소스, 페퍼로니 올려서 주세요.
        JSON 응답:
        {
          "size": "small",
          "type": "normal",
          "ingredients": ["cheese", "tomato sauce", "pepperoni"]
        }

        예시1:
        큰 피자 하나, 토마토 소스랑 바질, 모짜렐라 올려서 주세요.
        JSON 응답:
        {
          "size": "large",
          "type": "normal",
          "ingredients": ["tomato sauce", "basil", "mozzarella"]
        }

        고객 주문: %s""".formatted(order);

    Prompt prompt = Prompt.builder()
        .content(strPrompt)
        .build();

    // LLM으로 요청하고 응답을 받음
    String pizzaOrderJson = chatClient.prompt(prompt)
        .options(ChatOptions.builder()
            .temperature(0.0)
            .maxTokens(300)
            .build())
        .call()
        .content();

    return pizzaOrderJson;
  }
}
