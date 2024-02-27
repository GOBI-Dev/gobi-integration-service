package com.gobi.repository;

import com.gobi.model.PayrollReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PayrollReferenceRepository extends JpaRepository<PayrollReference, Long>, JpaSpecificationExecutor<PayrollReference> {

}
