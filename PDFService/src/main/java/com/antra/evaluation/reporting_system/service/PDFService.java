package com.antra.evaluation.reporting_system.service;

import com.antra.evaluation.reporting_system.pojo.api.PDFRequest;
import com.antra.evaluation.reporting_system.pojo.report.PDFFile;

import java.io.FileNotFoundException;

public interface PDFService {
    PDFFile createPDF(PDFRequest request);

    void deletePDF(String id);
}
