package com.antra.report.client.repository;

import com.antra.report.client.entity.ReportRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRequestRepo extends JpaRepository<ReportRequestEntity, String> {
    Page<ReportRequestEntity> findAll(Pageable var1);
}
