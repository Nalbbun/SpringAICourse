package com.example.demo.dto;

import lombok.Data;

@Data
public class ReviewClassification {
  // ##### 열거 타입 선언 #####
  public enum Sentiment {
    POSITIVE, NEUTRAL, NEGATIVE
  }

  // ##### 필드 선언 #####
  private String review;
  private Sentiment classification;
}
