package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
  @GetMapping("/")
  public String home() {
    return "home";
  }

  @GetMapping("/date-time-tools")
  public String dateTimeTools() {
    return "date-time-tools";
  }  

  @GetMapping("/heating-system-tools")
  public String heatingSystemTools() {
    return "heating-system-tools";
  }

  @GetMapping("/recommend-movie-tools")
  public String recommendMovieTools() {
    return "recommend-movie-tools";
  }

  @GetMapping("/exception-handling")
  public String exceptionHandling() {
    return "exception-handling";
  }  

  @GetMapping("/boom-barrier-tools")
  public String boomBarrierTools() {
    return "boom-barrier-tools";
  }   

  @GetMapping("/file-system-tools")
  public String fileSystemTools() {
    return "file-system-tools";
  }    

  @GetMapping("/internet-search-tools")
  public String internetSearchTools() {
    return "internet-search-tools";
  }   
}
