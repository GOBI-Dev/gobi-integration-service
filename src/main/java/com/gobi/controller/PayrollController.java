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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("payroll")
public class PayrollController {

    @Autowired
    PayrollService payrollService;

    @PostMapping("postPayrollBasic")
    public ResponseEntity<?> postPayrollBasic() throws ParseException {

        boolean result = payrollService.postPayrollBasic();
        if(result){
            return ResponseEntity.ok().body("Success");
        }else{
            return ResponseEntity.badRequest().body("Failed");
        }
    }

    @PostMapping("postPayroll")
    public ResponseEntity<?> postPayroll(@RequestBody List<PayrollInputDTO> payrollList, @RequestHeader HttpHeaders headers) throws ParseException {
        String key = Objects.requireNonNull(headers.get("GOBI-API-KEY")).get(0);
        if(key.equals("69399a188366b8fc8e59b5f0cb127e23a20b65b9")){
            boolean result = payrollService.postPayroll(payrollList);
            if(result){
                return ResponseEntity.ok().body("Success");
            }else{
                return ResponseEntity.badRequest().body("Failed");
            }
        }else{
            return ResponseEntity.badRequest().body("Request is not authenticated!");
        }
    }

    @GetMapping
    public ResponseEntity<?> findAll(@SearchSpec(caseSensitiveFlag = false) Specification<Payroll> specs,
                                     Pageable pageable) {
        Page<Payroll> integrationECopyCredits = payrollService.findAll(specs, pageable);
        return ResponseEntity.ok().body(integrationECopyCredits);
    }
}
