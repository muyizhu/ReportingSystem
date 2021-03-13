package com.antra.evaluation.reporting_system.Entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ExcelFileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private String fileId;

    @Column(name = "filename")
    private String fileName;

    @Column(name = "filelocation")
    private String fileLocation;

    @Column(name = "submitter")
    private String submitter;

    @Column(name = "filesize")
    private Long fileSize;

    @Column(name = "description")
    private String description;

    @Column(name = "generatedTime")
    private LocalDateTime generatedTime;

    public Long getFileSize() {
        return fileSize;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileLocation() {
        return fileLocation;
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    public String getSubmitter() {
        return submitter;
    }

    public void setSubmitter(String submitter) {
        this.submitter = submitter;
    }

    public LocalDateTime getGeneratedTime() {
        return generatedTime;
    }

    public void setGeneratedTime(LocalDateTime generatedTime) {
        this.generatedTime = generatedTime;
    }

    @Override
    public String toString() {
        return "ExcelFile{" +
                "fileId='" + fileId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileLocation='" + fileLocation + '\'' +
                ", submitter='" + submitter + '\'' +
                ", fileSize=" + fileSize +
                ", generatedTime=" + generatedTime +
                '}';
    }
}
