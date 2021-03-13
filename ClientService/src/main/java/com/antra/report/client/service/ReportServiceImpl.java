package com.antra.report.client.service;

import com.amazonaws.services.s3.AmazonS3;
import com.antra.report.client.entity.ExcelReportEntity;
import com.antra.report.client.entity.PDFReportEntity;
import com.antra.report.client.entity.ReportRequestEntity;
import com.antra.report.client.entity.ReportStatus;
import com.antra.report.client.exception.RequestNotFoundException;
import com.antra.report.client.pojo.EmailType;
import com.antra.report.client.pojo.FileType;
import com.antra.report.client.pojo.reponse.*;
import com.antra.report.client.pojo.request.ReportRequest;
import com.antra.report.client.repository.ReportRequestRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final ReportRequestRepo reportRequestRepo;
    private final SNSService snsService;
    private final AmazonS3 s3Client;
    private final EmailService emailService;

    private final RestTemplate restTemplate;




    public ReportServiceImpl(ReportRequestRepo reportRequestRepo, SNSService snsService, AmazonS3 s3Client, EmailService emailService, RestTemplate restTemplate) {
        this.reportRequestRepo = reportRequestRepo;
        this.snsService = snsService;
        this.s3Client = s3Client;
        this.emailService = emailService;
        this.restTemplate = restTemplate;
    }

    private ReportRequestEntity persistToLocal(ReportRequest request) {
        request.setReqId("Req-"+ UUID.randomUUID().toString());

        ReportRequestEntity entity = new ReportRequestEntity();
        entity.setReqId(request.getReqId());
        entity.setSubmitter(request.getSubmitter());
        entity.setDescription(request.getDescription());
        entity.setCreatedTime(LocalDateTime.now());

        PDFReportEntity pdfReport = new PDFReportEntity();
        pdfReport.setRequest(entity);
        pdfReport.setStatus(ReportStatus.PENDING);
        pdfReport.setCreatedTime(LocalDateTime.now());
        entity.setPdfReport(pdfReport);

        ExcelReportEntity excelReport = new ExcelReportEntity();
        BeanUtils.copyProperties(pdfReport, excelReport);
        entity.setExcelReport(excelReport);

        return reportRequestRepo.save(entity);
    }
    @Override
    public String loadBalanceTest(String name) {
        return restTemplate.getForObject("http://ExcelService/hi?name="+name,String.class);
    }

    @Override
    public ReportVO generateReportsSync(ReportRequest request) {

        persistToLocal(request);
        log.info("success stored reprot to local");
        //following method may become async
        sendDirectRequests(request);
        log.info("finish sending sendDirectRequests");
        return new ReportVO(reportRequestRepo.findById(request.getReqId()).orElseThrow());
    }
    //TODO:Change to parallel process using Threadpool? CompletableFuture?
    private void sendDirectRequests(ReportRequest request) {
        RestTemplate rs = this.restTemplate;
        ExcelResponse excelResponse = new ExcelResponse();
        PDFResponse pdfResponse = new PDFResponse();
        //supplyAsync: used default thread pool
        CompletableFuture<ExcelResponse> excelfuture = CompletableFuture.supplyAsync(()->rs.postForEntity("http://ExcelService/excel", request, ExcelResponse.class).getBody());

        //can set up call back, where future cannot


        //excelfuture.thenAccept(this::updateLocal);
        CompletableFuture<PDFResponse> pdffuture = CompletableFuture.supplyAsync(()->rs.postForEntity("http://PDFService/pdf", request, PDFResponse.class).getBody());
        //pdffuture.thenAccept(this::updateLocal);
        try {
            log.info("send excelResponse");
            excelResponse = excelfuture.get();

        } catch(Exception e) {
            log.error("Excel Generation Error (Sync) : e", e);
            excelResponse.setReqId(request.getReqId());
            excelResponse.setFailed(true);
        }
       finally {
            log.info("complete excelResponse");
            updateLocal(excelResponse);
        }
        try {
            pdfResponse = pdffuture.get();
        } catch(Exception e){
            log.error("PDF Generation Error (Sync) : e", e);
            pdfResponse.setReqId(request.getReqId());
            pdfResponse.setFailed(true);
        }
        finally {
            updateLocal(pdfResponse);
        }
    }


    private void updateLocal(ExcelResponse excelResponse) {
        SqsResponse response = new SqsResponse();
        BeanUtils.copyProperties(excelResponse, response);
        updateAsyncExcelReport(response);
    }
    private void updateLocal(PDFResponse pdfResponse) {
        SqsResponse response = new SqsResponse();
        BeanUtils.copyProperties(pdfResponse, response);
        updateAsyncPDFReport(response);
    }

    @Override
    @Transactional
    public ReportVO generateReportsAsync(ReportRequest request) {
        ReportRequestEntity entity = persistToLocal(request);
        snsService.sendReportNotification(request);           //post async req dealer
        log.info("Send SNS the message: {}",request);
        return new ReportVO(entity);
    }




    @Override
//    @Transactional // why this? email could fail
    public void updateAsyncPDFReport(SqsResponse response) {       //SQS response handler,update status
        ReportRequestEntity entity = reportRequestRepo.findById(response.getReqId()).orElseThrow(RequestNotFoundException::new);
        var pdfReport = entity.getPdfReport();
        pdfReport.setUpdatedTime(LocalDateTime.now());
        if (response.isFailed()) {
            pdfReport.setStatus(ReportStatus.FAILED);
        } else{
            pdfReport.setStatus(ReportStatus.COMPLETED);
            pdfReport.setFileId(response.getFileId());
            pdfReport.setFileLocation(response.getFileLocation());
            pdfReport.setFileSize(response.getFileSize());
        }
        entity.setUpdatedTime(LocalDateTime.now());
        reportRequestRepo.save(entity);
        String to = "zhumuyi1234@gmail.com";
        emailService.sendEmail(to, EmailType.SUCCESS, entity.getSubmitter());
    }

    @Override
//    @Transactional
    public void updateAsyncExcelReport(SqsResponse response) {
        ReportRequestEntity entity = reportRequestRepo.findById(response.getReqId()).orElseThrow(RequestNotFoundException::new);
        var excelReport = entity.getExcelReport();
        excelReport.setUpdatedTime(LocalDateTime.now());
        if (response.isFailed()) {
            excelReport.setStatus(ReportStatus.FAILED);
        } else{
            excelReport.setStatus(ReportStatus.COMPLETED);
            excelReport.setFileId(response.getFileId());
            excelReport.setFileLocation(response.getFileLocation());
            excelReport.setFileSize(response.getFileSize());
        }
        entity.setUpdatedTime(LocalDateTime.now());
        reportRequestRepo.save(entity);
        String to = "youremail@gmail.com";
        emailService.sendEmail(to, EmailType.SUCCESS, entity.getSubmitter());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportVO> getReportList() {  // local get dealer
        return reportRequestRepo.findAll().stream().map(ReportVO::new).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReportVO> getPaginatedReportList(Integer page,Integer size,String orderBy) {  // local get dealer
        reportRequestRepo.findAll().stream().map(ReportVO::new).collect(Collectors.toList());

        Sort sort = null;
        if (orderBy != null) {
            sort = Sort.by(Sort.Direction.ASC, orderBy);
        }
        Page<ReportRequestEntity> page1 = reportRequestRepo.findAll(PageRequest.of(page, size, sort));
        List<ReportVO> list = page1.getContent().stream().map(ReportVO::new).collect(Collectors.toList());
        PagedResponse<ReportVO> result = new PagedResponse<>();
        result.setPage(page1.getNumber());
        result.setRows(page1.getSize());
        result.setTotalPage(page1.getTotalPages());
        result.setTotalElement(page1.getTotalElements());
        result.setOrder(page1.getSort().toString());
        result.setBody(list);
        return result;
    }

    @Override
    public InputStream getFileBodyByReqId(String reqId, FileType type) {
        ReportRequestEntity entity = reportRequestRepo.findById(reqId).orElseThrow(RequestNotFoundException::new);
        if (type == FileType.PDF) {
            String fileLocation = entity.getPdfReport().getFileLocation(); // this location is s3 "bucket/key"
            String bucket = fileLocation.split("/")[0];
            String key = fileLocation.split("/")[1];
            return s3Client.getObject(bucket, key).getObjectContent();
        } else if (type == FileType.EXCEL) {
            log.info("file need to download is EXCEL");
            String fileId = entity.getExcelReport().getFileId();
//            String fileLocation = entity.getExcelReport().getFileLocation();
//            try {
//                return new FileInputStream(fileLocation);// this location is in local, definitely sucks
//            } catch (FileNotFoundException e) {
//                log.error("No file found", e);
//            }
  //          RestTemplate restTemplate = new RestTemplate();
//            InputStream is = restTemplate.execute(, HttpMethod.GET, null, ClientHttpResponse::getBody, fileId);
            log.info("send Excel download request to ExcelService");
            ResponseEntity<Resource> exchange = restTemplate.exchange("http://ExcelService/excel/{id}/content",
                    HttpMethod.GET, null, Resource.class, fileId);
            try {
                log.info("finish sending Excel downloading request");
                return exchange.getBody().getInputStream();
            } catch (IOException e) {
                log.error("Cannot download excel",e);
            }
        }
        return null;
    }

    @Override
    //    @Transactional
    public void deleteFileBodyByReqId(String reqId, FileType type) {
        ReportRequestEntity entity = reportRequestRepo.findById(reqId).orElseThrow(RequestNotFoundException::new);
        reportRequestRepo.deleteById(reqId);
        if (type == FileType.PDF) {
            PDFReportEntity pdfReportEntity = entity.getPdfReport();
/*            String fileLocation = pdfReportEntity.getFileLocation(); // this location is s3 "bucket/key"
            String bucket = fileLocation.split("/")[0];
            String key = fileLocation.split("/")[1];
            s3Client.deleteObject(bucket,key);*/
          ResponseEntity<PDFResponse> exchange = restTemplate.exchange("http://PDFService/pdf/{id}",
                    HttpMethod.DELETE, null, PDFResponse.class, pdfReportEntity.getFileId());

            try {
                PDFResponse response = exchange.getBody();

                if (exchange.getBody().isFailed()) {
                    throw new IOException("delete PDF fail");
                }

            } catch (IOException e) {
                log.error("Cannot delete pdf", e);
            } catch (NullPointerException e) {
                log.error("ReportServicelmpl-delete pdf-PDFServer did not response");
            }

        } else if (type == FileType.EXCEL) {
            String fileId = entity.getExcelReport().getFileId();
//            String fileLocation = entity.getExcelReport().getFileLocation();
//            try {
//                return new FileInputStream(fileLocation);// this location is in local, definitely sucks
//            } catch (FileNotFoundException e) {
//                log.error("No file found", e);
//            }

//            InputStream is = restTemplate.execute(, HttpMethod.GET, null, ClientHttpResponse::getBody, fileId);
            ResponseEntity<ExcelResponse> exchange =
                restTemplate.exchange("http://ExcelService/excel/{id}",
                        HttpMethod.DELETE, null, ExcelResponse.class, fileId);

            try {
                ExcelResponse response = exchange.getBody();
                if (  exchange.getBody().isFailed()) {
                    throw new IOException("delete excel fail");
                }

            } catch (IOException e) {
                log.error("Cannot delete excel", e);
            } catch (NullPointerException e) {
                log.error("ReportServicelmpl-delete excel-ExcelServer did not response");
            }
        }
    }
}
