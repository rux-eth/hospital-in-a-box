package com.hospitalinabox.domain.repository;

import com.hospitalinabox.domain.entity.AuditLogEntity;
import com.hospitalinabox.domain.entity.Hl7MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    List<AuditLogEntity> findByHl7Message(Hl7MessageEntity hl7Message);
}