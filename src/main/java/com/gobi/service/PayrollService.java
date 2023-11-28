package com.gobi.service;

import com.gobi.dto.PayrollInputDTO;
import com.gobi.model.Payroll;
import com.gobi.repository.PayrollRepository;
import com.gobi.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

@Service
public class PayrollService {

    @Autowired
    PayrollRepository payrollRepository;
    @Autowired
    RestTemplate restTemplate;

    public Page<Payroll> findAll(Specification<Payroll> specs, Pageable pageable) {
        return payrollRepository.findAll(Specification.where(specs), pageable);
    }

    //Dansnii medeelluudiig tatah
    public List<PayrollInputDTO> getPayrolls()
    {
        HttpEntity<PayrollInputDTO[]> request = new HttpEntity<>(Util.payrollHeaders());
        ResponseEntity<PayrollInputDTO[]> responseEntity;

        try{
            responseEntity = restTemplate.exchange(
                    Util.payrollUrl,
                    HttpMethod.GET, request, PayrollInputDTO[].class);

            return List.of(Objects.requireNonNull(responseEntity.getBody()));

        }catch (Exception e){
            return null;
        }
    }

    //Tatsan dansnii medeelluudiig хадгалах
    public boolean postPayroll() throws ParseException {
        List<PayrollInputDTO> payrollDTOList = getPayrolls();
        List<Payroll> result = new ArrayList<>();
        if(payrollDTOList!=null) {
            if (payrollDTOList.size() > 0) {
                for (PayrollInputDTO payrollInputDTO : payrollDTOList) {
                    Payroll payroll = new Payroll();
                    if(payrollInputDTO.getDepartmentId()!=null&&!payrollInputDTO.getDepartmentId().equals("")){
                        payroll.setDepartmentId(payrollInputDTO.getDepartmentId());}

                    if(payrollInputDTO.getDepartmentName()!=null&&!payrollInputDTO.getDepartmentName().equals("")){
                        payroll.setDepartmentName(payrollInputDTO.getDepartmentName());
                    }

                    if(payrollInputDTO.getVoucherDate()!=null&&!payrollInputDTO.getVoucherDate().equals("")){
                        payroll.setVoucherDate(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(payrollInputDTO.getVoucherDate() + " 00:00:00"));
                    }

                    if(payrollInputDTO.getVoucherId()!=null&&!payrollInputDTO.getVoucherId().equals("")){
                        payroll.setVoucherId(Long.valueOf(payrollInputDTO.getVoucherId()));
                    }

                    if(payrollInputDTO.getAccountId()!=null&&!payrollInputDTO.getAccountId().equals("")){
                        payroll.setAccountId(payrollInputDTO.getAccountId());
                    }

                    if(payrollInputDTO.getDtAmt()!=null&&!payrollInputDTO.getDtAmt().equals("")){
                        payroll.setDtAmt(new BigDecimal(payrollInputDTO.getDtAmt()));
                    }

                    if(payrollInputDTO.getKtAmt()!=null&&!payrollInputDTO.getKtAmt().equals("")){
                        payroll.setKtAmt(new BigDecimal(payrollInputDTO.getKtAmt()));
                    }

                    if(payrollInputDTO.getVoucherNote()!=null&&!payrollInputDTO.getVoucherNote().equals("")){
                        payroll.setVoucherNote(payrollInputDTO.getVoucherNote());
                    }

                    if(payrollInputDTO.getEmployeeId()!=null&&!payrollInputDTO.getEmployeeId().equals("")){
                        payroll.setEmployeeId(Long.valueOf(payrollInputDTO.getEmployeeId()));
                    }

                    if(payrollInputDTO.getEmployeeName()!=null&&!payrollInputDTO.getEmployeeName().equals("")){
                        payroll.setEmployeeName(payrollInputDTO.getEmployeeName());
                    }
                    LocalDate currentDate = LocalDate.now();
                    payroll.setSYear((long) currentDate.getYear());
                    payroll.setSMonth((long) currentDate.getMonthValue());
                    payroll.setSDay((long) currentDate.getDayOfMonth());

                    payroll.setCreatedDate(Calendar.getInstance().getTime());
                    payroll.setStatus(true);
                    payroll.setIsLog(false);
                    result.add(payroll);
                }
                payrollRepository.saveAll(result);
            }
            return true;
        }else{
           return false;
        }
    }

    public boolean postPayrollForTest(List<PayrollInputDTO> payrollInputDTOList) throws ParseException {
        if(payrollInputDTOList !=null) {
            if (payrollInputDTOList.size() > 0) {
                for (PayrollInputDTO payrollInputDTO : payrollInputDTOList) {
                    System.out.println("payroll one by one: "+ payrollInputDTO);

                    Payroll payroll = new Payroll();
                    if(payrollInputDTO.getDepartmentId()!=null&&!payrollInputDTO.getDepartmentId().equals("")){
                        payroll.setDepartmentId(payrollInputDTO.getDepartmentId());}

                    if(payrollInputDTO.getDepartmentName()!=null&&!payrollInputDTO.getDepartmentName().equals("")){
                        payroll.setDepartmentName(payrollInputDTO.getDepartmentName());
                    }

                    if(payrollInputDTO.getVoucherDate()!=null&&!payrollInputDTO.getVoucherDate().equals("")){
                        payroll.setVoucherDate(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(payrollInputDTO.getVoucherDate() + " 00:00:00"));
                    }

                    if(payrollInputDTO.getVoucherId()!=null&&!payrollInputDTO.getVoucherId().equals("")){
                        payroll.setVoucherId(Long.valueOf(payrollInputDTO.getVoucherId()));
                    }

                    if(payrollInputDTO.getAccountId()!=null&&!payrollInputDTO.getAccountId().equals("")){
                        payroll.setAccountId(payrollInputDTO.getAccountId());
                    }

                    if(payrollInputDTO.getDtAmt()!=null&&!payrollInputDTO.getDtAmt().equals("")){
                        payroll.setDtAmt(new BigDecimal(payrollInputDTO.getDtAmt()));
                    }

                    if(payrollInputDTO.getKtAmt()!=null&&!payrollInputDTO.getKtAmt().equals("")){
                        payroll.setKtAmt(new BigDecimal(payrollInputDTO.getKtAmt()));
                    }

                    if(payrollInputDTO.getVoucherNote()!=null&&!payrollInputDTO.getVoucherNote().equals("")){
                        payroll.setVoucherNote(payrollInputDTO.getVoucherNote());
                    }

                    if(payrollInputDTO.getEmployeeId()!=null&&!payrollInputDTO.getEmployeeId().equals("")){
                        payroll.setEmployeeId(Long.valueOf(payrollInputDTO.getEmployeeId()));
                    }

                    if(payrollInputDTO.getEmployeeName()!=null&&!payrollInputDTO.getEmployeeName().equals("")){
                       payroll.setEmployeeName(payrollInputDTO.getEmployeeName());
                    }

                    LocalDate currentDate = LocalDate.now();
                    payroll.setSYear((long) currentDate.getYear());
                    payroll.setSMonth((long) currentDate.getMonthValue());
                    payroll.setSDay((long) currentDate.getDayOfMonth());

                    payroll.setCreatedDate(Calendar.getInstance().getTime());
                    payroll.setStatus(true);
                    payroll.setIsLog(false);
                    payrollRepository.save(payroll);
                }
            }
            return true;
        }else{
            return false;
        }
    }
}
