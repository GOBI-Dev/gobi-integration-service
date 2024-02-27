package com.gobi.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;
import java.math.BigDecimal;


@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "plm_post")
public class Plm extends BaseModel{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String materialCode;
    private String materialDescription;
    private String unitOfMeasureBaseUnit;
    private String materialType;
    private String laboratoryDesignOfficeCategory;
    private String materialGroupCategorySubCategory;
    private String division;
    private String industryStandartDescriptionDepartment;
    private String grossWeight;
    private String weightUnit;
    private String netWeight;
    private String externalMaterialGroupBrand;
    private String basicMaterialStyle;
    private String materialLongDescription;
    private String size;
    private String color;
    private String season;
    private String collection;
    private String gauge;
    private String pattern;
    private String mainMaterialComposition;
    private String yarnCount;
    private String onlineCode;
    private String colorName;
    private String kidsClassification;
    private Long plmYear;
    private Long plmMonth;
    private Long plmDay;
    private String materialName;
    private String additionalInformation;
    private String sapMaterialDescription;
    private String sapMaterialLongDescription;
}
