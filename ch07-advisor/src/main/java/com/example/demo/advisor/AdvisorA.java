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
public class AdvisorA implements CallAdvisor, StreamAdvisor {
  @Override
  public String getName() { 
    return this.getClass().getSimpleName();
  }

  @Override
  public int getOrder() { 
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }

  @Override
  public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
    log.info("[전처리]");
    ChatClientResponse response = chain.nextCall(request);
    log.info("[후처리]");
    return response;
  }

  @Override
  public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
    log.info("[전처리]");
    Flux<ChatClientResponse> response = chain.nextStream(request); 
    return response; 
  }
}