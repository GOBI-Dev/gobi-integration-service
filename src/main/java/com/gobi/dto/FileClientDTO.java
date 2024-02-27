package com.gobi.dto;

import lombok.Data;

@Data
public class FileClientDTO {

    private Long id;
    private String name;
    private String description;
    private String uuid;
    private String format;
    private String parentUuid;
}
