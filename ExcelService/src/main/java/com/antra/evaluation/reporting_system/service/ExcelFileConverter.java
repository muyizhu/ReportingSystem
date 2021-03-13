package com.antra.evaluation.reporting_system.service;

import com.antra.evaluation.reporting_system.Entity.ExcelFileEntity;
import com.antra.evaluation.reporting_system.pojo.report.ExcelFile;

public interface ExcelFileConverter {
    public ExcelFile convert(ExcelFileEntity e);

    public ExcelFileEntity convert(ExcelFile e);
}
