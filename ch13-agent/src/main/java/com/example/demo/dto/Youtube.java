package com.example.demo.dto;

import lombok.Data;

// YouTube 비디오 정보
@Data
public class Youtube {
    // 비디오 제목
    private String title;
    
    // 업로드 날짜 (예: "2024-12-23")
    private String uploadDate;
    
    // YouTube 링크 (예: "https://www.youtube.com/watch?v=...")
    private String link;
}
