package com.gobi.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "Payroll_Post_FROM_INTERACTIVE_IBI")
public class Payroll extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String departmentId;
    private String departmentName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date voucherDate;

    private Long voucherId;
    private String accountId;

    private BigDecimal dtAmt;
    private BigDecimal ktAmt;

    private String voucherNote;
    private Long employeeId;
    private String employeeName;

    private Long sYear;
    private Long sMonth;
    private Long sDay;

}
