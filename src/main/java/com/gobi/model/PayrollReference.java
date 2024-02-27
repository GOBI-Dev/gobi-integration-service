package com.gobi.model;

import lombok.Data;
import javax.persistence.*;
@Entity
@Data
@Table(name = "Payroll_Reference")
public class PayrollReference {

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

}
