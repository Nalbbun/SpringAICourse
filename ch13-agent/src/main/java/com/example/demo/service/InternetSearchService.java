package com.example.demo.service;

/**
 * 인터넷 검색 서비스 인터페이스
 * 다양한 검색 구현체를 사용할 수 있도록 추상화
 */
public interface InternetSearchService {
  
  /**
   * 검색 수행
   * 
   * @param query 검색 쿼리
   * @return 검색 결과 문자열
   */
  String search(String query);
  
  /**
   * 웹 페이지 내용 가져오기
   * 
   * @param url 웹 페이지 URL
   * @return 페이지 내용
   */
  String fetch(String url);
  
  /**
   * 호출 횟수 초기화 (선택적 구현)
   * ReAct 패턴 테스트를 위한 메서드
   */
  default void resetCallCount() {
    // 기본 구현은 아무 것도 하지 않음
  }

  String fetchWebPageContent(String url);
}
