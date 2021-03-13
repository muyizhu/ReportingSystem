package com.antra.evaluation.reporting_system.service;

import com.antra.evaluation.reporting_system.Entity.ExcelFileEntity;
import com.antra.evaluation.reporting_system.pojo.report.ExcelFile;

public class ExcelFileConverterlmpl implements ExcelFileConverter{

    @Override
    public ExcelFile convert(ExcelFileEntity entity){
        ExcelFile f = new ExcelFile();
        f.setFileId(entity.getFileId());
        f.setDescription(entity.getDescription());
        f.setFileLocation(entity.getFileLocation());
        f.setFileName(entity.getFileName());
        f.setFileSize(entity.getFileSize());
        f.setGeneratedTime(entity.getGeneratedTime());
        f.setSubmitter(entity.getSubmitter());
        return f;
    }

    @Override
    public ExcelFileEntity convert(ExcelFile file) {
        ExcelFileEntity entity = new ExcelFileEntity();
        entity.setFileId(file.getFileId());
        entity.setDescription(file.getDescription());
        entity.setFileLocation(file.getFileLocation());
        entity.setFileName(file.getFileName());
        entity.setFileSize(file.getFileSize());
        entity.setGeneratedTime(file.getGeneratedTime());
        entity.setSubmitter(file.getSubmitter());
        return entity;
    }
}
