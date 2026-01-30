package com.example.demo.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.netty.http.client.HttpClient;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import lombok.extern.slf4j.Slf4j;

// Google Custom Search API 공통 서비스
// @Service
@Slf4j
public class GoogleSearchService implements InternetSearchService {
  //--------------------------------------------------------------------------------
  private final WebClient searchWebClient;
  private WebClient htmlWebClient;
  private final String apiKey;
  private final String engineId;
  private final ObjectMapper objectMapper = new ObjectMapper();

  //--------------------------------------------------------------------------------
  public GoogleSearchService(
      WebClient.Builder webClientBuilder,
      @Value("${google.search.endpoint}") String endpoint,
      @Value("${google.search.apiKey}") String apiKey,
      @Value("${google.search.engineId}") String engineId) {
    
    this.apiKey = apiKey;
    this.engineId = engineId;
    
    // WebClient 설정: Google Search API용
    this.searchWebClient = webClientBuilder
        .baseUrl(endpoint)
        .defaultHeader("Accept", "application/json")
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
        .build();
    
    // WebClient 설정: HTML 페이지 가져오기용 (SSL 검증 비활성화)
    try {
      var sslContext = SslContextBuilder
          .forClient()
          .trustManager(InsecureTrustManagerFactory.INSTANCE)
          .build();
      var httpClient = HttpClient.create()
          .secure(t -> t.sslContext(sslContext));
      
      this.htmlWebClient = WebClient.builder()
          .clientConnector(new ReactorClientHttpConnector(httpClient))
          .defaultHeader("Accept", "text/html")
          .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
          .build();
    } catch (Exception e) {
      // SSL 설정 실패 시 기본 WebClient 사용
      this.htmlWebClient = WebClient.builder()
          .defaultHeader("Accept", "text/html")
          .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
          .build();
    }
  }

  //--------------------------------------------------------------------------------
  // Google Custom Search API 호출 - Agent 친화적 형식으로 반환
  @Override
  public String search(String query) {
    try {
      // Google Custom Search API 호출
      String responseBody = searchWebClient.get()
          .uri(uriBuilder -> uriBuilder
              .queryParam("key", apiKey)
              .queryParam("cx", engineId)
              .queryParam("q", query)
              .queryParam("num", 10)
              .build())
          .retrieve()
          .onStatus(status -> status.value() == 403,
              response -> response.bodyToMono(String.class)
                  .map(body -> new RuntimeException("Google API 할당량이 초과되었습니다.")))
          .onStatus(status -> status.value() == 429,
              response -> response.bodyToMono(String.class)
                  .map(body -> new RuntimeException("Google API 요청 한도에 도달했습니다.")))
          .bodyToMono(String.class)
          .block();
      
      // JSON 파싱 및 Agent 친화적 형식으로 변환
      return formatSearchResults(responseBody, query);
      
    } catch (Exception e) {
      return "검색 오류: " + e.getMessage();
    }
  }

  //--------------------------------------------------------------------------------
  // JSON 응답을 Agent가 이해하기 쉬운 텍스트 형식으로 변환
  private String formatSearchResults(String jsonResponse, String query) {
    try {
      JsonNode root = objectMapper.readTree(jsonResponse);
      JsonNode items = root.get("items");
      
      if (items == null || items.size() == 0) {
        return String.format("'%s'에 대한 검색 결과가 없습니다.", query);
      }
      
      StringBuilder result = new StringBuilder();
      int index = 1;
      
      for (JsonNode item : items) {
        String title = item.get("title").asText();
        String link = item.get("link").asText();
        String snippet = item.get("snippet").asText();
        
        // Agent가 파싱하기 쉬운 형식으로 포맷팅
        result.append(String.format("[%d] %s\n", index, title));
        result.append(String.format("%s\n", link));
        result.append(String.format("%s\n", snippet));
        
        // 스니펫에서 가격 정보 추출 시도
        String price = extractPriceFromText(snippet);
        if (price != null) {
          result.append(String.format("가격: %s\n", price));
        }
        
        result.append("\n");
        index++;
      }
      
      return result.toString();
      
    } catch (Exception e) {
      return "검색 결과 파싱 오류: " + e.getMessage();
    }
  }

  //--------------------------------------------------------------------------------
  // 웹 페이지의 HTML을 가져와 텍스트로 변환 + 가격 정보 추출
  @Override
  public String fetchWebPageContent(String url) {
    try {
      // WebClient로 HTML 가져오기
      String html = htmlWebClient.get()
          .uri(url)
          .retrieve()
          .bodyToMono(String.class)
          .block();

      if (html == null || html.isBlank()) {
        return "웹페이지 내용을 가져올 수 없습니다.";
      }
      
      // Jsoup로 HTML 파싱 및 텍스트 추출
      Document doc = Jsoup.parse(html);
      String bodyText = doc.body().text();
      
      // 텍스트가 너무 길면 제한 (LLM 토큰 고려)
      if (bodyText.length() > 3000) {
        bodyText = bodyText.substring(0, 3000) + "...";
      }
      
      // 가격 정보 추출
      String priceInfo = extractPriceFromText(bodyText);
      if (priceInfo != null) {
        bodyText = bodyText + "\n\n가격 정보: " + priceInfo;
      }
      
      return bodyText;
      
    } catch (Exception e) {
      return "웹페이지 크롤링 오류: " + e.getMessage();
    }
  }
  
  //--------------------------------------------------------------------------------
  // 텍스트에서 가격 정보 추출
  private String extractPriceFromText(String text) {
    // 다양한 가격 패턴 매칭
    Pattern[] patterns = {
        Pattern.compile("(\\d{1,3}(?:,\\d{3})*원)"),           // 10,000원
        Pattern.compile("(\\$\\d{1,3}(?:,\\d{3})*)"),          // $50
        Pattern.compile("(₩\\d{1,3}(?:,\\d{3})*)"),            // ₩30000
        Pattern.compile("입장료[:\\s]*(\\d{1,3}(?:,\\d{3})*원?)"), // 입장료: 5000원
        Pattern.compile("가격[:\\s]*(\\d{1,3}(?:,\\d{3})*원?)"),  // 가격: 15000원
        Pattern.compile("1인[\\s]*(\\d{1,3}(?:,\\d{3})*원?)")    // 1인 12000원
    };
    
    for (Pattern pattern : patterns) {
      Matcher matcher = pattern.matcher(text);
      if (matcher.find()) {
        String price = matcher.group(1);
        // 숫자만 있는 경우 "원" 추가
        if (!price.contains("원") && !price.contains("$") && !price.contains("₩")) {
          price = price + "원";
        }
        return price;
      }
    }
    
    return null;
  }

  @Override
  public String fetch(String url) {
	// TODO Auto-generated method stub
	return null;
  }
}
