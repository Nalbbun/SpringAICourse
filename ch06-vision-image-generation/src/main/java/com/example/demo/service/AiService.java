package com.example.demo.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AiService {
  private ChatClient chatClient;

  @Autowired
  private ImageModel imageModel;

  public AiService(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  // ##### 이미지 분석 메소드 #####
  public Flux<String> imageAnalysis(String question, String contentType, byte[] bytes) {
    // 시스템 메시지 생성
    SystemMessage systemMessage = SystemMessage.builder()
        .text("""
          당신은 이미지 분석 전문가입니다.   
          사용자 질문에 맞게 이미지를 분석하고 답변을 한국어로 하세요. 
        """)
        .build();

    // 미디어 생성
    Media media = Media.builder()
        .mimeType(MimeType.valueOf(contentType))
        .data(new ByteArrayResource(bytes))
        .build();

    // 사용자 메시지 생성
    UserMessage userMessage = UserMessage.builder()
        .text(question)
        .media(media)
        .build();

    // 프롬프트 생성
    Prompt prompt = Prompt.builder()
        .messages(systemMessage, userMessage)
        .build();

    // LLM에 요청하고, 응답받기
    Flux<String> flux = chatClient.prompt(prompt)
        .stream()
        .content();
    return flux;
  }

  // ##### 한글 문장을 영어 문장으로 번역하는 메소드 #####
  private String koToEn(String text) {
    String question = """
          당신은 번역사입니다. 아래 한글 문장을 영어 문장으로 번역해주세요.
          %s
        """.formatted(text);

    // UserMessage 생성
    UserMessage userMessage = UserMessage.builder()
        .text(question)
        .build();

    // Prompt 생성
    Prompt prompt = Prompt.builder()
        .messages(userMessage)
        .build();

    // LLM을 호출하고 텍스트 답변 얻기
    String englishDescription = chatClient.prompt(prompt).call().content();
    return englishDescription;
  }

  // ##### 이미지를 새로 생성하는 메소드 #####
  public String generateImage(String description) {
    // 한글 질문을 영어 질문으로 번역
    String englishDescription = koToEn(description);

    // 이미지 설명을 포함하는 ImageMessage 생성
    ImageMessage imageMessage = new ImageMessage(englishDescription);

    // gpt-image-1 옵션 설정
    OpenAiImageOptions imageOptions = OpenAiImageOptions.builder()
        .model("gpt-image-1")
        .quality("low")
        .width(1536)
        .height(1024)
        .N(1)
        .build();
  
    // dall-e 시리즈 옵션 설정
    // OpenAiImageOptions imageOptions = OpenAiImageOptions.builder()
    //     // dall-e 시리즈 옵션
    //     .model("dall-e-3")
    //     .responseFormat("b64_json")
    //     .width(1024)
    //     .height(1024)
    //     .N(1)
    //     .build();        

    // 프롬프트 생성
    List<ImageMessage> imageMessageList = List.of(imageMessage);
    ImagePrompt imagePrompt = new ImagePrompt(imageMessageList, imageOptions);

    // 모델 호출 및 응답 받기
    ImageResponse imageResponse = imageModel.call(imagePrompt);
    
    // base64로 인코딩된 이미지 문자열 얻기
    String b64Json = imageResponse.getResult().getOutput().getB64Json();
    return b64Json;
  }

  // ##### 원본 이미지를 편집하는 메소드 #####
  public String editImage(String description, byte[] originalImage, byte[] maskImage) { 
    // 한글 질문을 영어 질문으로 번역
    String englishDescription = koToEn(description);

    // 원본 이미지를 ByteArrayResource로 생성
    ByteArrayResource originalRes = new ByteArrayResource(originalImage) {
      @Override
      public String getFilename() {
        return "image.png"; // 가상 파일 이름 반환(확장명으로 타입 정보 획득)
      }
    };

    // 마스크 이미지를 ByteArrayResource로 생성
    ByteArrayResource maskRes = new ByteArrayResource(maskImage) {
      @Override
      public String getFilename() {
        return "mask.png"; // 가상 파일 이름 반환
      }
    };

    // 이미지 모델 옵션 설정
    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
    form.add("model", "gpt-image-1");
    form.add("image", originalRes);
    form.add("mask", maskRes);
    form.add("prompt", englishDescription);
    form.add("n", "1");
    form.add("size", "1536x1024");
    form.add("quality", "high");

    // WebClient 생성
    WebClient webClient = WebClient.builder()
        // 이미지 편집을 위한 요청 URL
        .baseUrl("https://api.openai.com/v1/images/edits")
        // 인증 헤더 설정
        .defaultHeader("Authorization", "Bearer " + System.getenv("OPENAI_API_KEY"))
        // 전략을 적용해서 메모리를 늘림
        .exchangeStrategies(ExchangeStrategies.builder()
          .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(10 * 1536 * 1024))
          .build())
        .build();

    // 비동기 단일값(OpenAIImageEditResponse) 스트림인 Mono 얻기
    Mono<OpenAIImageEditResponse> mono = webClient.post()
        // multipart/form-data 형식으로 전송
        .contentType(MediaType.MULTIPART_FORM_DATA)
        // 요청 본문에 form 데이터를 넣음
        .body(BodyInserters.fromMultipartData(form))
        // 응답 받기
        .retrieve()
        // 응답 본문의 JSON을 OpenAIImageEditResponse 타입으로 역직렬화해서
        // 비동기 단일값(OpenAIImageEditResponse) 스트림인 Mono로 반환
        .bodyToMono(OpenAIImageEditResponse.class);

    // Mono가 완료될 때까지 현재 스레드를 블로킹하고,
    // 동기 방식으로 단일값 OpenAIImageEditResponse를 얻음
    OpenAIImageEditResponse response = mono.block();

    // 레코드로부터 base64로 인코딩된 이미지 문자열 얻기
    String b64Json = response.data().get(0).b64_json();
    return b64Json;

    // 클래스로부터 base64로 인코딩된 이미지 문자열 얻기
    // String b64Json = response.getData().get(0).getB64_json();
    // return b64Json;
  }

  // 레코드로 역직렬화할 경우
  // {"data": [{"url": "xxxxx", "b64_json": "xxxxx"}, ... ]}
  // 선언된 필드 외에 JSON에 포함된 속성들을 무시
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record OpenAIImageEditResponse(List<Image> data) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Image(
        String url,
        String b64_json) {
    }
  }

  // 클래스로 역직렬화할 경우
  // {"data": [{"url": "xxxxx", "b64_json": "xxxxx"}, ... ]}  
  // @Data
  // @JsonIgnoreProperties(ignoreUnknown = true)
  // public static class OpenAIImageEditResponse {
  //   private List<Image> data;
  //   @Data
  //   @JsonIgnoreProperties(ignoreUnknown = true)
  //   public static class Image {
  //     private String url;
  //     private String b64_json;
  //   }
  // }
}
