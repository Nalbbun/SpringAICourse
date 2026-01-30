package com.example.demo.boombarrier;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/ai")
@Slf4j
public class BoomBarrierController {
  // ##### 필드 #####
  @Autowired
  private BoomBarrierService boomBarrierService;

  // ##### 요청 매핑 메소드 #####
  @PostMapping(
    value = "/boom-barrier-tools", 
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE, 
    produces = MediaType.TEXT_PLAIN_VALUE)
  public String boomBarrierTools(
      @RequestParam("attach") MultipartFile attach) throws IOException {
    String answer = boomBarrierService.chat(
        attach.getContentType(), attach.getBytes());
    return answer;
  }
}
