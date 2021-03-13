package com.antra.evaluation.reporting_system.repo;

import com.antra.evaluation.reporting_system.Entity.ExcelFileEntity;
import com.antra.evaluation.reporting_system.pojo.report.ExcelFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MyExcelRepository extends JpaRepository<ExcelFileEntity,String> {

}
