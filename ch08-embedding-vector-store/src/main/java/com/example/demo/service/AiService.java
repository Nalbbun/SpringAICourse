package com.example.demo.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiService {
  // ##### 필드 #####
  @Autowired
  private EmbeddingModel embeddingModel;

  @Autowired
  private VectorStore vectorStore;

  // ##### 메소드 #####
  public void textEmbedding(String question) {
    // 임베딩하기
    EmbeddingResponse response = embeddingModel.embedForResponse(List.of(question));

    // 임베딩 모델 정보 얻기
    EmbeddingResponseMetadata metadata = response.getMetadata();
    log.info("모델 이름: {}", metadata.getModel());
    log.info("모델의 임베딩 차원: {}", embeddingModel.dimensions());

    // 임베딩 결과 얻기
    Embedding embedding = response.getResults().get(0);
    log.info("벡터 차원: {}", embedding.getOutput().length);
    log.info("벡터: {}", embedding.getOutput());
  }

  // public void textEmbedding(String question) {
  //   // 임베딩하기
  //   float[] vector = embeddingModel.embed(question);
  //   log.info("벡터 차원: {}", vector.length);
  //   log.info("벡터: {}", vector);
  // }

  public void addDocument() {
    // Document 목록 생성
    List<Document> documents = List.of(
        new Document("대통령 선거는 5년마다 있습니다.", Map.of("source", "헌법", "year", 1987)),
        new Document("대통령 임기는 4년입니다.", Map.of("source", "헌법", "year", 1980)),
        new Document("국회의원은 법률안을 심의·의결합니다.", Map.of("source", "헌법", "year", 1987)),
        new Document("자동차를 사용하려면 등록을 해야합니다.", Map.of("source", "자동차관리법")),
        new Document("대통령은 행정부의 수반입니다.", Map.of("source", "헌법", "year", 1987)),
        new Document("국회의원은 4년마다 투표로 뽑습니다.", Map.of("source", "헌법", "year", 1987)),
        new Document("승용차는 정규적인 점검이 필요합니다.", Map.of("source", "자동차관리법")));

    // 벡터 저장소에 저장
    vectorStore.add(documents);
  }

  public List<Document> searchDocument1(String question) {
    List<Document> documents = vectorStore.similaritySearch(question);
    return documents;
  }

  public List<Document> searchDocument2(String question) {
    List<Document> documents = vectorStore.similaritySearch(
        SearchRequest.builder()
            .query(question)
            .topK(1)
            .similarityThreshold(0.4)
            .filterExpression("source == '헌법' && year >= 1987")
            .build());
    return documents;
  }

  // public List<Document> searchDocument2(String question) {
  //   FilterExpressionBuilder feb = new FilterExpressionBuilder();

  //   List<Document> documents = vectorStore.similaritySearch(
  //       SearchRequest.builder()
  //           .query(question)
  //           .topK(1)
  //           .similarityThreshold(0.4)
  //           .filterExpression(feb
  //               .and(
  //                   feb.eq("source", "헌법"),
  //                   feb.gte("year", 1987))
  //               .build())
  //           .build());
  //   return documents;
  // }

  public void deleteDocument() {
    vectorStore.delete("source == '헌법' && year < 1987");
  }
}
