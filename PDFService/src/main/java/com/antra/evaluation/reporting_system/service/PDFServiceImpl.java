package com.antra.evaluation.reporting_system.service;

import com.amazonaws.services.s3.AmazonS3;
import com.antra.evaluation.reporting_system.pojo.api.PDFRequest;
import com.antra.evaluation.reporting_system.pojo.report.PDFFile;
import com.antra.evaluation.reporting_system.repo.PDFRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class PDFServiceImpl implements PDFService {

    private static final Logger log = LoggerFactory.getLogger(PDFServiceImpl.class);

    private final PDFRepository repository;

    private final PDFGenerator generator;

    private final AmazonS3 s3Client;

    @Value("${s3.bucket}")
    private String s3Bucket;

    public PDFServiceImpl(PDFRepository repository, PDFGenerator generator, AmazonS3 s3Client) {
        this.repository = repository;
        this.generator = generator;
        this.s3Client = s3Client;
    }

    @Override
    public PDFFile createPDF(final PDFRequest request) {
        PDFFile file = new PDFFile();
        String id = UUID.randomUUID().toString();
        log.info("creating PDF file，UUID is "+id);
        file.setId("File-" + id);
        file.setSubmitter(request.getSubmitter());
        file.setDescription(request.getDescription());
        file.setGeneratedTime(LocalDateTime.now());

        PDFFile generatedFile= generator.generate(request);

        File temp = new File(generatedFile.getFileLocation());
        log.debug("Upload temp file to s3 {}", generatedFile.getFileLocation());
        s3Client.putObject(s3Bucket,file.getId(),temp);
        log.debug("Uploaded");

        file.setFileLocation(String.join("/",s3Bucket,file.getId()));
        file.setFileSize(generatedFile.getFileSize());
        file.setFileName(generatedFile.getFileName());
        repository.save(file);

        log.debug("clear tem file {}", file.getFileLocation());
        if(temp.delete()){
            log.debug("cleared");
        }

        return file;
    }

    @Override
    public void deletePDF(String id)  {
        Optional<PDFFile> pdfOption = repository.findById(id);
        pdfOption.orElseThrow(()->new NoSuchElementException("can not find pdf ID"));
        repository.deleteById(id);
        PDFFile pdf = pdfOption.get();
        String fileLocation = pdf.getFileLocation(); // this location is s3 "bucket/key"
        String bucket = fileLocation.split("/")[0];
        String key = fileLocation.split("/")[1];
        s3Client.deleteObject(bucket, key);
    }

}