package com.hospitalinabox.domain.repository;

import com.hospitalinabox.domain.entity.Hl7MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface Hl7MessageRepository extends JpaRepository<Hl7MessageEntity, UUID> {

    Optional<Hl7MessageEntity> findByMessageControlId(String messageControlId);
}