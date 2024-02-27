package com.gobi.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "Payroll_Excel")
public class PayrollExcel extends BaseModel{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String registryNumber;
    private String name;
    private Long year;
    private Long period;
    private String position;
    private String division;
    private String costCenter;
    private String employeeCode;
    private BigDecimal employeeWages;
    private BigDecimal socialInsuranceFromEmployee;
    private BigDecimal socialInsuranceFromCompany;
    private BigDecimal personalIncomeTax;
    private BigDecimal advancePayroll;
    private BigDecimal arFromEmployee;
    private BigDecimal tradeUnionFee;
    private Long serialNo;
}
