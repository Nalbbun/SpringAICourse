package com.example.demo.dto;

import lombok.Data;

// 숙소 정보
@Data
public class Accommodation {
    // 숙소 이름 (예: "제주 프리미엄 호텔", "바다 뷰 펜션")
    private String name;
    
    // 숙소 주소 (예: "제주시 노형동", "서귀포시 중문관광로")
    private String address;
    
    // 숙소 설명 및 특징 (예: "오션뷰 더블", "스파, 피트니스 센터 완비")
    private String description;
    
    // 1박 요금 (단위: 원, 예: 150000, 80000)
    private Integer pricePerNight;
}
