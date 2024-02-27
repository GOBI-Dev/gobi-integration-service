package com.gobi.dto;

import lombok.Data;

@Data
public class PlmResponseDTO {
    private Long code;
    private String message;
    private PlmResponseDataDTO data;
}
