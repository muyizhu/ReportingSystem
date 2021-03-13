package com.antra.report.client.controller;

import com.antra.report.client.pojo.FileType;
import com.antra.report.client.pojo.reponse.ErrorResponse;
import com.antra.report.client.pojo.reponse.GeneralResponse;
import com.antra.report.client.pojo.reponse.PagedResponse;
import com.antra.report.client.pojo.reponse.ReportVO;
import com.antra.report.client.pojo.request.ReportRequest;
import com.antra.report.client.service.ReportService;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

@RestController
@Api(value = "User", description = "REST API for generating reports", tags={"User"})
public class ReportController {
    //http://localhost:8080/swagger-ui.html#/
    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;


    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public ResponseEntity<GeneralResponse> FallBack(){
        return ResponseEntity.ok(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,"server fail"));
    }

    @GetMapping("/report")
    @HystrixCommand(fallbackMethod = "FallBack")
    @ApiOperation(value = "method description")
    public ResponseEntity<GeneralResponse> listReport() {
        log.info("Got Request to list all report");
        return ResponseEntity.ok(new GeneralResponse(reportService.getReportList()));
    }


    @GetMapping("/PageReport")
    @HystrixCommand(fallbackMethod = "FallBack")
    @ApiOperation(value = "method description")
    public ResponseEntity<PagedResponse<ReportVO>> listPaginatedReport(@RequestParam(required = false, defaultValue = "0") Integer pageNo,
                                                              @RequestParam(required = false, defaultValue = "5") Integer rows,
                                                              @RequestParam(required = false, defaultValue = "reqId") String orderBy) {
        log.info("Got Request to list all report");
        return ResponseEntity.ok(reportService.getPaginatedReportList(pageNo,rows,orderBy));
    }



    @PostMapping("/report/sync")
    @HystrixCommand(fallbackMethod = "FallBack")
    @ApiOperation(value = "method description")
    public ResponseEntity<GeneralResponse> createReportDirectly(@RequestBody @Validated ReportRequest request) {
        log.info("Got Request to generate report - sync: {}", request);

        request.setDescription(String.join(" - ", "Sync", request.getDescription()));
        return ResponseEntity.ok(new GeneralResponse(reportService.generateReportsSync(request)));
    }

    @PostMapping("/report/async")
    @HystrixCommand(fallbackMethod = "FallBack")
    @ApiOperation(value = "method description")
    public ResponseEntity<GeneralResponse> createReportAsync(@RequestBody @Validated ReportRequest request) {
        log.info("Got Request to generate report - async: {}", request);
        request.setDescription(String.join(" - ", "Async", request.getDescription()));
        reportService.generateReportsAsync(request);
        return ResponseEntity.ok(new GeneralResponse());
    }

    @GetMapping("/report/content/{reqId}/{type}")
    @HystrixCommand(fallbackMethod = "FallBack")
    @ApiOperation(value = "method description")
    public void downloadFile(@PathVariable String reqId, @PathVariable FileType type, HttpServletResponse response) throws IOException {
        log.debug("Got Request to Download File - type: {}, reqid: {}", type, reqId);
        InputStream fis = reportService.getFileBodyByReqId(reqId, type);
        String fileType = null;
        String fileName = null;
        if(type == FileType.PDF) {
            fileType = "application/pdf";
            fileName = "report.pdf";
        } else if (type == FileType.EXCEL) {
            fileType = "application/vnd.ms-excel";
            fileName = "report.xls";
        }
        response.setHeader("Content-Type", fileType);
        response.setHeader("fileName", fileName);
        if (fis != null) {
            FileCopyUtils.copy(fis, response.getOutputStream());
        } else{
            response.setStatus(500);
        }
        log.debug("Downloaded File:{}", reqId);
    }

    @DeleteMapping("/report/{reqId}/{type}")
    @HystrixCommand(fallbackMethod = "FallBack")
    @ApiOperation(value = "method description")
    public ResponseEntity<GeneralResponse> deleteFile(@PathVariable String reqId, @PathVariable FileType type) throws IOException {
        log.debug("deletion Request - type: {}, reqid: {}", type, reqId);
//        try{
//            reportService.deleteFileBodyByReqId(reqId,type);
//            return ResponseEntity.ok(new GeneralResponse());
//        }
//        catch(Exception e){
//            log.error(e.toString());
//            GeneralResponse gr = new GeneralResponse();
//            gr.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
//
//            return ResponseEntity.ok(gr);
//        }
        reportService.deleteFileBodyByReqId(reqId,type);
        return ResponseEntity.ok(new GeneralResponse());
    }

//   @DeleteMapping
//   @PutMapping


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GeneralResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Input Data invalid: {}", e.getMessage());
        String errorFields = e.getBindingResult().getFieldErrors().stream().map(fe -> String.join(" ",fe.getField(),fe.getDefaultMessage())).collect(Collectors.joining(", "));
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST, errorFields), HttpStatus.BAD_REQUEST);
    }

    @GetMapping(value = "/hi")
    public String hi(@RequestParam(required = false, defaultValue = "ClientServer") String name) {
        System.out.println("asdfasd");
        return reportService.loadBalanceTest( name );
    }
}
