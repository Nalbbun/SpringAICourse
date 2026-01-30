package com.example.demo.service;

import java.util.List;
import java.util.Objects;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiServiceStepBackPrompt {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiServiceStepBackPrompt(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  // ##### 메소드 #####
  public String stepBackPrompt(String question) throws Exception {
    String questions = chatClient.prompt()
        .user("""
            사용자 질문을 처리할 때 Step-Back 프롬프트 기법을 사용하려고 합니다.
            사용자 질문을 단계별 질문들로 재구성해주세요. 
            맨 마지막 질문은 사용자 질문과 일치해야 합니다.
            단계별 질문을 항목으로 하는 JSON 배열로 출력해 주세요.
            예시: ["...", "...", "...", "..."]
            사용자 질문: %s
            """.formatted(question))
        .call()
        .content();
  
    String json = questions.substring(questions.indexOf("["), questions.indexOf("]")+1);
    log.info(json);
    
    ObjectMapper objectMapper = new ObjectMapper();
    List<String> listQuestion = objectMapper.readValue(
        json,
        new TypeReference<List<String>>() {}
    );
    
    String[] answerArray = new String[listQuestion.size()];
    for(int i=0; i<listQuestion.size(); i++) {
      String stepQuestion = listQuestion.get(i);
      String stepAnswer = getStepAnswer(stepQuestion, answerArray);
      answerArray[i] = stepAnswer;
      log.info("단계{} 질문: {}, 답변: {}", i+1, stepQuestion, stepAnswer);
    }
    
    return answerArray[answerArray.length-1];
  }

  public String getStepAnswer(String question, String... prevStepAnswers) {
    String context = "";
    for (String prevStepAnswer : prevStepAnswers) {
      context += Objects.requireNonNullElse(prevStepAnswer, "");
    }
    String answer = chatClient.prompt()
        .user("""
            %s
            문맥: %s
            """.formatted(question, context))
        .call()
        .content();
    return answer;
  }
}
