package com.example.demo.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.core.Ordered;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
public class AdvisorC implements CallAdvisor, StreamAdvisor {
  @Override
  public String getName() { 
    return this.getClass().getSimpleName();
  }

  @Override
  public int getOrder() { 
    return Ordered.HIGHEST_PRECEDENCE + 3;
  }

  @Override
  public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
    log.info("[전처리]");
    ChatClientResponse advisedResponse = chain.nextCall(request);
    log.info("[후처리]");
    return advisedResponse;
  }

  @Override
  public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
    //전처리
    log.info("[전처리]");
    Flux<ChatClientResponse> flux = chain.nextStream(request);
    return flux; 
  }
}