package com.example.demo.dto;

import lombok.Data;

// 맛집 정보
@Data
public class Restaurant {
    // 맛집 이름 (예: "제주 해산물 전문점", "흑돼지 전문점")
    private String name;
    
    // 맛집 주소 (예: "제주시 중앙로", "서귀포시 표선면")
    private String address;
    
    // 맛집 설명 및 대표 메뉴 (예: "성게국, 전복죽", "흑돼지 구이")
    private String description;
    
    // 1인 평균 가격 (예: 18,000, 25,000)
    private Integer price;
}
