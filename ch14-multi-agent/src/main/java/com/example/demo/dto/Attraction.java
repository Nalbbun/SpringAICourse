package com.example.demo.dto;

import lombok.Data;

// 관광지 정보
@Data
public class Attraction {
    // 관광지 이름 (예: "제주 동문재래시장", "성산일출봉")
    private String name;
    
    // 관광지 주소 (예: "제주시 조천읍", "서귀포시 성산읍")
    private String address;
    
    // 관광지 설명 및 특징 (예: "제주 대표 전통시장, 현지 먹거리 체험")
    private String description;
    
    // 입장료 정보 (예: 0=무료, 5000, 10000)
    private Integer entranceFee;
}
