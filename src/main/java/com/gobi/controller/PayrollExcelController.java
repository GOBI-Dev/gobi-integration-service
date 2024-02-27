package com.gobi.controller;

import com.gobi.model.PayrollExcel;
import com.gobi.service.PayrollExcelService;
import com.sipios.springsearch.anotation.SearchSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.text.ParseException;

@Slf4j
@RestController
@RequestMapping("payrollExcel")
public class PayrollExcelController {

    @Autowired
    PayrollExcelService payrollService;

    @GetMapping
    public ResponseEntity<?> findAll(@SearchSpec(caseSensitiveFlag = false) Specification<PayrollExcel> specs,
                                     Pageable pageable) {
        Page<PayrollExcel> integrationECopyCredits = payrollService.findAll(specs, pageable);
        return ResponseEntity.ok().body(integrationECopyCredits);
    }

    @PostMapping("postPayrollExcel")
    public ResponseEntity<String> postPayrollExcel(@RequestParam("file") MultipartFile multipartFile) throws ParseException, InterruptedException {
                return payrollService.postPayrollExcelMultipart(multipartFile);
    }

    @PostMapping("workbook")
    public ResponseEntity<String> postPayrollExcel(@RequestParam("path") String path) throws ParseException, IOException {
        return payrollService.postPayrollExcelWorkBook(path);
    }

}
