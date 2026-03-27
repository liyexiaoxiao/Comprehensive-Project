package com.donffroodus.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.donffroodus.user_service.entity.OperationLog;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
}