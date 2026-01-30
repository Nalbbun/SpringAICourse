package com.example.demo.tool;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BoomBarrierTools {
  // ##### 도구 #####
  @McpTool(description = "차단기를 올립니다.")
  public String boomBarrierUp() {
    log.info("차단기를 올립니다.");
    return "차단기 올림";
  }

  @McpTool(description = "차단기를 내립니다.")
  public String boomBarrierDown() {
    log.info("차단기를 내립니다.");
    return "차단기 내림";
  }
}
