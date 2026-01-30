package com.example.demo.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaxCharLengthAdvisor implements CallAdvisor {
  // ##### 필드 #####
  public static final String MAX_CHAR_LENGTH = "maxCharLength";
  private int maxCharLength = 300; //공통 최대 문자 수
  private int order;

  // #### 생성자 #####
  public MaxCharLengthAdvisor(int order) {
    this.order = order;
  }

  // ##### 메소드 #####
  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  @Override
  public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
    // 전처리 작업: 사용자 메시지가 강화된 ChatClientRequest 얻기
    ChatClientRequest mutatedRequest = augmentPrompt(request);
    // 다음 Advisor 호출 또는 LLM으로 요청
    ChatClientResponse response = chain.nextCall(mutatedRequest);
    // 응답 반환
    return response;
  }

  // 사용자 메시지 강화
  private ChatClientRequest augmentPrompt(ChatClientRequest request) {
    // 추가할 사용자 텍스트 얻기
    String userText = this.maxCharLength + "자 이내로 답변해주세요.";    
    Integer maxCharLength = (Integer) request.context().get(MAX_CHAR_LENGTH);
    if (maxCharLength != null) {
      userText = maxCharLength + "자 이내로 답변해주세요.";
    }
    String finalUserText = userText;
    
    // 사용자 메시지를 강화한 Prompt 얻기
    Prompt originalPrompt = request.prompt();
    Prompt augmentedPrompt = originalPrompt.augmentUserMessage(
        userMessage -> UserMessage.builder()
            .text(userMessage.getText() + " " + finalUserText)
            .build());

    // 수정된 ChatClientRequest 얻기
    ChatClientRequest mutatedRequest = request.mutate()
        .prompt(augmentedPrompt)
        .build();
    return mutatedRequest;
  }
}