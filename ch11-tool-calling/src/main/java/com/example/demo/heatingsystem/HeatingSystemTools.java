package com.example.demo.heatingsystem;

import java.util.Random;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HeatingSystemTools {

  @Tool(description = 
    """
      타겟 온도까지 난방 시스템을 가동합니다.
      난방 시스템 가동이 성공되었을 경우 success를 반환합니다.
      난방 시스템 가동이 실패되었을 경우 failure를 반환합니다.
    """
  )
  public String startHeatingSystem(
    @ToolParam(description = "타겟 온도", required = true) int targetTemperature,
    ToolContext toolContext) {
    String controlKey = (String) toolContext.getContext().get("controlKey");
    if(controlKey!=null && controlKey.equals("heatingSystemKey")) {
      log.info("{}도까지 난방 시스템을 가동합니다.", targetTemperature);
      return "success";
    } else {
      log.info("난방 시스템을 가동할 권한이 없습니다.");
      return "failure";
    }
  } 

  @Tool(description = 
    """
    난방 시스템을 중지합니다.
    난방 시스템 중지가 성공되었을 경우 success를 반환합니다.
    난방 시스템 중지가 실패되었을 경우 failure를 반환합니다.
    """
  )
  public String stopHeatingSystem(ToolContext toolContext) {
    String controlKey = (String) toolContext.getContext().get("controlKey");
    if(controlKey!=null && controlKey.equals("heatingSystemKey")) {
      log.info("난방 시스템을 중지합니다.");
      return "success";
    } else {
      log.info("난방 시스템을 중지할 권한이 없습니다.");
      return "failure";
    }
  } 

  @Tool(description = "현재 온도를 제공합니다.")
  public int getTemperature() {
    Random random = new Random();
    int temperature = random.nextInt(13) + 18;
    log.info("현재 온도: {}", temperature);
    return temperature;
  }   

}
