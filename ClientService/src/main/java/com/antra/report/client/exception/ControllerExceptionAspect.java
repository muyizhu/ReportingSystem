package com.antra.report.client.exception;

import com.antra.report.client.controller.ReportController;
import com.antra.report.client.pojo.reponse.GeneralResponse;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ControllerExceptionAspect {
    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    @Pointcut("execution(public * com.antra.report.client.controller.ReportController.deleteFile(..))")
    public void deleteFile(){}


    @AfterThrowing(pointcut="deleteFile()",throwing="ex")
    public ResponseEntity<GeneralResponse> deleteFileHandler(Exception ex){
        log.error(ex.toString());
        GeneralResponse gr = new GeneralResponse();
        gr.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

        return ResponseEntity.ok(gr);
    }

}
