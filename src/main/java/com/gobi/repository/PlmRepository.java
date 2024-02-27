package com.gobi.repository;

import com.gobi.model.Plm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlmRepository extends JpaRepository<Plm, Long>, JpaSpecificationExecutor<Plm> {
    List<Plm> findAllByCreatedDateIsNotNullOrderByIdDesc();
}
