package com.example.demo.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.AiService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ai")
@Slf4j
public class AiController {
  // ##### 필드 #####
  @Autowired
  private AiService aiService;

  // ##### 요청 매핑 메소드 #####
  @PostMapping(value = "/chat", 
      consumes = MediaType.APPLICATION_JSON_VALUE, 
      produces = MediaType.TEXT_PLAIN_VALUE)
  public Flux<String> chat(@RequestBody Map<String, String> map) {
    Flux<String> answer = aiService.chat(map.get("question"));
    return answer;
  }  

  @PostMapping(value = "/boom-barrier", 
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE, 
      produces = MediaType.TEXT_PLAIN_VALUE)
  public Flux<String> boomBarrier(@RequestPart("attach") FilePart attach) throws IOException {
    // 파일의 MIME 타입 얻기(image/jpeg)
    String contentType = attach.headers().getContentType().toString();
    
    // 파일 데이터(Flux<DataBuffer>)를 한 번에 합쳐서 Mono<DataBuffer> 로 만듬
    Mono<DataBuffer> monoDataBuffer = DataBufferUtils.join(attach.content());
    
    Flux<String> answer =  monoDataBuffer.flatMapMany(dataBuffer -> {
      try {
        // DataBuffer 내용을 모두 읽어 byte[] 에 담음
        int size = dataBuffer.readableByteCount();
        byte[] bytes = new byte[size];
        dataBuffer.read(bytes);
        //AiService의 boomBarrier() 호출
        Flux<String> fluxString = aiService.boomBarrier(contentType, bytes);
        return fluxString;
      } finally {
        // DataBuffer는 명시적으로 release 해줌
        DataBufferUtils.release(dataBuffer);
      }
    });
    
    return answer;
  }
}
