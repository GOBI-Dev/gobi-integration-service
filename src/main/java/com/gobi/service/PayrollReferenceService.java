package com.gobi.service;

import com.gobi.model.PayrollReference;
import com.gobi.repository.PayrollReferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PayrollReferenceService {

    @Autowired
    PayrollReferenceRepository payrollReferenceRepository;
    public List<PayrollReference> findAll() {
        return payrollReferenceRepository.findAll();
    }

}
