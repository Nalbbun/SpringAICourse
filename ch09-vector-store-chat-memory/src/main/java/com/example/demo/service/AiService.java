package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiService {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiService(
      JdbcTemplate jdbcTemplate,
      EmbeddingModel embeddingModel,
      ChatClient.Builder chatClientBuilder) {

    VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
        .initializeSchema(false)
        .schemaName("public")
        .vectorTableName("chat_memory_vector_store")
        .build();

    this.chatClient = chatClientBuilder
        .defaultAdvisors(
            VectorStoreChatMemoryAdvisor.builder(vectorStore)
            //.defaultTopK(5) //유사도 검색 후에 가져오는 메시지 수, 기본 20개
            .build(),
            new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
          )
        .build();

  }

  public String chat(String userText, String conversationId) {
    String answer = chatClient.prompt()
        .user(userText)
        .advisors(advisorSpec -> advisorSpec.param(
            ChatMemory.CONVERSATION_ID, conversationId))
        .call()
        .content();
    return answer;
  }
}
