package com.antra.evaluation.reporting_system.repo;

import com.antra.evaluation.reporting_system.Entity.ExcelFileEntity;
import com.antra.evaluation.reporting_system.pojo.report.ExcelFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


public interface ExcelRepository{
    ExcelFileEntity saveFile(ExcelFileEntity file);

    ExcelFileEntity deleteFile(String id);

    List<ExcelFileEntity> getFiles();

    Optional<ExcelFileEntity> getFileById(String id);

}
