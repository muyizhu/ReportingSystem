package com.antra.evaluation.reporting_system.repo;

import com.antra.evaluation.reporting_system.Entity.ExcelFileEntity;
import com.antra.evaluation.reporting_system.pojo.report.ExcelFile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ExcelRepositoryImpl implements ExcelRepository {

    MyExcelRepository rp;

    @Override
    public ExcelFileEntity saveFile(ExcelFileEntity file) {
        rp.save(file);
        return file;
    }

    @Override
    public ExcelFileEntity deleteFile(String id) {
        Optional<ExcelFileEntity> f = rp.findById(id);
        ExcelFileEntity file = f.orElseThrow();
        return file;
    }

    @Override
    public List<ExcelFileEntity> getFiles() {
        return rp.findAll();
    }

    @Override
    public Optional<ExcelFileEntity> getFileById(String id) {
        return rp.findById(id);
    }
}

