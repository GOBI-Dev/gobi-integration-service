package com.gobi.repository;

import com.gobi.model.PlmTransferLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Date;
import java.util.List;

@Repository
public interface PlmTransferLogRepository extends JpaRepository<PlmTransferLog, Long>, JpaSpecificationExecutor<PlmTransferLog> {

    @Query(value="select plmLog.transferDate FROM PlmTransferLog as plmLog " +
            "where plmLog.isSuccessful=true ORDER BY plmLog.transferDate DESC")
    List<Date> getLogDatesByDesc();

    Boolean existsByTransferDateAndFileNameAndIsSuccessfulIsFalse(Date transferDate, String fileName);
    PlmTransferLog getByTransferDateAndFileNameAndIsSuccessfulIsFalse(Date transferDate, String fileName);
}
