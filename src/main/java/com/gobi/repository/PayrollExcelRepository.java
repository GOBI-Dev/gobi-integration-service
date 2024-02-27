package com.gobi.repository;

import com.gobi.model.PayrollExcel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Date;
import java.util.List;

@Repository
public interface PayrollExcelRepository extends JpaRepository<PayrollExcel, Long>, JpaSpecificationExecutor<PayrollExcel> {
    Boolean existsByYearAndPeriodAndSerialNo(Long year, Long period, Long serialNo);
    @Query(value = "SELECT DISTINCT (pe.COST_CENTER) FROM Payroll_Excel AS pe " +
            "WHERE pe.SERIAL_NO=:serialNo AND pe.REGISTRY_NUMBER is null", nativeQuery = true)
    List<String> getCostCenters (Long serialNo);
    List<PayrollExcel> getByCostCenterAndSerialNo(String costCenter, Long serialNo);
    List<PayrollExcel> getByCreatedDateBetweenAndSerialNoAndEmployeeCodeIsNotNull(Date startDate, Date endDate, Long serialNo);

    @Query(value="select DISTINCT (pe.serialNo) FROM PayrollExcel as pe WHERE pe.year=:year AND pe.period=:period AND pe.arFromEmployee is null ORDER BY pe.serialNo desc")
    List<Long> getSerialNosByYearAndPeriodAndArFromEmployeeIsNull(Long year, Long period);
    @Query(value="select DISTINCT (pe.serialNo) FROM PayrollExcel as pe WHERE pe.year=:year AND pe.period=:period ORDER BY pe.serialNo desc")
    List<Long> getSerialNosByYearAndPeriod(Long year, Long period);

    @Query(value = "SELECT SUM(pe.EMPLOYEE_WAGES) FROM Payroll_Excel AS pe " +
            "WHERE pe.COST_CENTER=:costCenter AND pe.CREATED_DATE between :startDate AND :endDate AND pe.SERIAL_NO=:serialNo AND pe.REGISTRY_NUMBER is null", nativeQuery = true)
    long totalEmployeeWages (String costCenter, Date startDate, Date endDate, Long serialNo);

    @Query(value = "SELECT SUM(pe.EMPLOYEE_WAGES) FROM Payroll_Excel AS pe " +
            "WHERE pe.SERIAL_NO=:serialNo AND pe.REGISTRY_NUMBER is null", nativeQuery = true)
    long hulDunEmployeeWages (Long serialNo);

    @Query(value = "SELECT SUM(pe.SOCIAL_INSURANCE_FROM_EMPLOYEE) FROM Payroll_Excel AS pe " +
            "WHERE pe.COST_CENTER=:costCenter AND pe.CREATED_DATE between :startDate AND :endDate AND pe.SERIAL_NO=:serialNo AND pe.REGISTRY_NUMBER is null", nativeQuery = true)
    long totalSocialInsuranceFromEmployee (String costCenter, Date startDate, Date endDate, Long serialNo);

    @Query(value = "SELECT SUM(pe.SOCIAL_INSURANCE_FROM_EMPLOYEE) FROM Payroll_Excel AS pe " +
            "WHERE pe.SERIAL_NO=:serialNo AND pe.REGISTRY_NUMBER is null", nativeQuery = true)
    long hulDunSocialInsuranceFromEmployee (Long serialNo);

    @Query(value = "SELECT SUM(pe.SOCIAL_INSURANCE_FROM_COMPANY) FROM Payroll_Excel AS pe " +
            "WHERE pe.COST_CENTER=:costCenter AND pe.CREATED_DATE between :startDate AND :endDate AND pe.SERIAL_NO=:serialNo AND pe.REGISTRY_NUMBER is null", nativeQuery = true)
    long totalSocialInsuranceFromCompany (String costCenter, Date startDate, Date endDate, Long serialNo);

    @Query(value = "SELECT SUM(pe.SOCIAL_INSURANCE_FROM_COMPANY) FROM Payroll_Excel AS pe " +
            "WHERE pe.SERIAL_NO=:serialNo AND pe.REGISTRY_NUMBER is null", nativeQuery = true)
    long hulDunSocialInsuranceFromCompany (Long serialNo);

    @Query(value = "SELECT SUM(pe.PERSONAL_INCOME_TAX) FROM Payroll_Excel AS pe " +
            "WHERE pe.COST_CENTER=:costCenter AND pe.CREATED_DATE between :startDate AND :endDate AND pe.SERIAL_NO=:serialNo AND pe.REGISTRY_NUMBER is null", nativeQuery = true)
    long totalPersonalIncomeTax (String costCenter, Date startDate, Date endDate, Long serialNo);

    @Query(value = "SELECT SUM(pe.PERSONAL_INCOME_TAX) FROM Payroll_Excel AS pe " +
            "WHERE pe.SERIAL_NO=:serialNo AND pe.REGISTRY_NUMBER is null", nativeQuery = true)
    long hulDunIncomeTax (Long serialNo);

    @Query(value = "SELECT SUM(pe.ADVANCE_PAYROLL) FROM Payroll_Excel AS pe " +
            "WHERE pe.COST_CENTER=:costCenter AND pe.CREATED_DATE between :startDate AND :endDate AND pe.SERIAL_NO=:serialNo AND pe.REGISTRY_NUMBER is null", nativeQuery = true)
    long totalAdvancePayroll (String costCenter, Date startDate, Date endDate, Long serialNo);

    @Query(value = "SELECT SUM(pe.ADVANCE_PAYROLL) FROM Payroll_Excel AS pe " +
            "WHERE pe.SERIAL_NO=:serialNo AND pe.REGISTRY_NUMBER is null", nativeQuery = true)
    long hulDunAdvancePayroll (Long serialNo);

    @Query(value = "SELECT SUM(pe.AR_FROM_EMPLOYEE) FROM Payroll_Excel AS pe " +
            "WHERE pe.COST_CENTER=:costCenter AND pe.CREATED_DATE between :startDate AND :endDate AND pe.SERIAL_NO=:serialNo AND pe.REGISTRY_NUMBER is null", nativeQuery = true)
    long totalArFromEmployee (String costCenter, Date startDate, Date endDate, Long serialNo);
    @Query(value = "SELECT SUM(pe.AR_FROM_EMPLOYEE) FROM Payroll_Excel AS pe " +
            "WHERE pe.AR_FROM_EMPLOYEE is not null AND pe.CREATED_DATE between :startDate AND :endDate AND pe.SERIAL_NO=:serialNo AND pe.REGISTRY_NUMBER is not null", nativeQuery = true)
    long hulDunArFromEmployee (Date startDate, Date endDate, Long serialNo);

    @Query(value = "SELECT SUM(pe.TRADE_UNION_FEE) FROM Payroll_Excel AS pe " +
            "WHERE pe.COST_CENTER=:costCenter AND pe.CREATED_DATE between :startDate AND :endDate AND pe.SERIAL_NO=:serialNo AND pe.REGISTRY_NUMBER is null", nativeQuery = true)
    long totalTradeUnionFee (String costCenter, Date startDate, Date endDate, Long serialNo);

    @Query(value = "SELECT SUM(pe.TRADE_UNION_FEE) FROM Payroll_Excel AS pe " +
            "WHERE pe.SERIAL_NO=:serialNo AND pe.REGISTRY_NUMBER is null", nativeQuery = true)
    long hulDunTradeUnionFee (Long serialNo);
}
