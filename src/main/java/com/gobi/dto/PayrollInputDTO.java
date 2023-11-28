package com.gobi.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PayrollInputDTO {

    @JsonProperty("DepartmentId")
    private String DepartmentId;
    @JsonProperty("DepartmentName")
    private String DepartmentName;
    @JsonProperty("VoucherDate")
    private String VoucherDate;
    @JsonProperty("VoucherId")
    private String VoucherId;
    @JsonProperty("AccountId")
    private String AccountId;
    @JsonProperty("DtAmt")
    private String DtAmt;
    @JsonProperty("KtAmt")
    private String KtAmt;
    @JsonProperty("VoucherNote")
    private String VoucherNote;
    @JsonProperty("EmployeeId")
    private String EmployeeId;
    @JsonProperty("EmployeeName")
    private String EmployeeName;
}