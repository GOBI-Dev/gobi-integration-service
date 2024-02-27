package com.gobi.controller;

import com.gobi.model.Plm;
import com.gobi.service.PlmService;
import com.sipios.springsearch.anotation.SearchSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("plm")
public class PlmController {

    @Autowired
    PlmService plmService;

    @GetMapping
    public ResponseEntity<?> findAll(@SearchSpec(caseSensitiveFlag = false) Specification<Plm> specs, Pageable pageable) {
        Page<Plm> integrationECopyCredits = plmService.findAll(specs, pageable);
        return ResponseEntity.ok().body(integrationECopyCredits);
    }

    @PostMapping("postPlm")
    public ResponseEntity<?> save(@RequestParam String fileName) {
            boolean result = plmService.postPlm(fileName);
            if(result){
                return ResponseEntity.ok().body("Success");
            }else{
                return ResponseEntity.badRequest().body("Failed");
            }
    }

}
