package com.gobi.service;

import com.gobi.model.PlmTransferLog;
import com.gobi.repository.PlmTransferLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

@Service
public class PlmTransferLogService {

    @Autowired
    PlmTransferLogRepository plmTransferLogRepository;

    public Page<PlmTransferLog> findAll(Specification<PlmTransferLog> specs, Pageable pageable) {
        return plmTransferLogRepository.findAll(Specification.where(specs), pageable);
    }

    public PlmTransferLog save(PlmTransferLog plmTransferLog) {
        if(plmTransferLogRepository.existsByTransferDateAndFileNameAndIsSuccessfulIsFalse(plmTransferLog.getTransferDate(), plmTransferLog.getFileName())){
            PlmTransferLog plmTransferLog1 = plmTransferLogRepository.getByTransferDateAndFileNameAndIsSuccessfulIsFalse(plmTransferLog.getTransferDate(), plmTransferLog.getFileName());
            plmTransferLog1.setIsSuccessful(true);
            plmTransferLog1.setIsRetransfer(true);
            return plmTransferLogRepository.save(plmTransferLog1);
        }else{
            return plmTransferLogRepository.save(plmTransferLog);
        }
    }

    public Date getLogDatesByDesc(){
        if(plmTransferLogRepository.getLogDatesByDesc().size()>0){
            return plmTransferLogRepository.getLogDatesByDesc().get(0);
        }else {
            Calendar cal1 = Calendar.getInstance();
            cal1.add(Calendar.DAY_OF_MONTH, -1);
            cal1.add(Calendar.MINUTE, -1);
           return cal1.getTime();
        }
    }

}
