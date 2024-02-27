package com.gobi.controller;

import com.gobi.model.PlmTransferLog;
import com.gobi.service.PlmTransferLogService;
import com.sipios.springsearch.anotation.SearchSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@Slf4j
@RestController
@RequestMapping("plmTransferLog")
public class PlmTransferLogController {
    @Autowired
    PlmTransferLogService plmTransferLogService;

    @GetMapping
    public ResponseEntity<?> findAll(@SearchSpec(caseSensitiveFlag = false) Specification<PlmTransferLog> specs, Pageable pageable) {
        Page<PlmTransferLog> plmTransferLogs = plmTransferLogService.findAll(specs, pageable);
        return ResponseEntity.ok().body(plmTransferLogs);
    }

    @PostMapping
    public ResponseEntity<?> post(PlmTransferLog plmTransferLog) {
        plmTransferLog = plmTransferLogService.save(plmTransferLog);
        return ResponseEntity.ok().body(plmTransferLog);
    }

    @GetMapping("/getLastSuccessLogDate")
    public Date getLastSuccessLogDate(){
        return plmTransferLogService.getLogDatesByDesc();
    }
}
