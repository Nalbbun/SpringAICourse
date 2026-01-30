package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

// 개별 일정 항목
@Data
@NoArgsConstructor
public class ScheduleItem {
    // 활동 시작 시간 (예: "09:00", "13:00", "19:00", HH:MM 형식)
    private String time;

    // 타입 (필수값): "attractions" (관광지), "meals" (식사), "accommodation" (숙박)
    @JsonProperty(required = true)
    private String type;
    
    // 장소 또는 활동 이름 (예: "제주 동문재래시장", "해산물 전문점", "프리미엄 호텔")
    private String name;
    
    // 상세 주소 (예: "제주시 조천읍", "서귀포시 중앙로")
    private String address;
    
    // 활동 설명 또는 메뉴/객실 정보 (예: "제주 대표 전통시장", "성게국, 전복죽", "오션뷰 더블")
    private String description;
    
    // 예상 비용 (단위: 원, 입장료/식사비/숙박비 등)
    private Integer cost;
}
