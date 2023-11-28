package com.gobi.controller;

import com.gobi.dto.PayrollInputDTO;
import com.gobi.model.Payroll;
import com.gobi.service.PayrollService;
import com.sipios.springsearch.anotation.SearchSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("payroll")
public class PayrollController {

    @Autowired
    PayrollService payrollService;

    @PostMapping("postPayroll")
    public ResponseEntity<?> postPayroll() throws ParseException {

        boolean result = payrollService.postPayroll();
        if(result){
            return ResponseEntity.ok().body("Success");
        }else{
            return ResponseEntity.badRequest().body("Failed");
        }
    }

    @PostMapping("postPayrollForTest")
    public ResponseEntity<?> postPayrollForTest(@RequestBody List<PayrollInputDTO> payrollList) throws ParseException {

        boolean result = payrollService.postPayrollForTest(payrollList);
        if(result){
            return ResponseEntity.ok().body("Success");
        }else{
            return ResponseEntity.badRequest().body("Failed");
        }
    }

    @GetMapping
    public ResponseEntity<?> findAll(@SearchSpec(caseSensitiveFlag = false) Specification<Payroll> specs,
                                     Pageable pageable) {
        Page<Payroll> integrationECopyCredits = payrollService.findAll(specs, pageable);
        return ResponseEntity.ok().body(integrationECopyCredits);
    }
}
