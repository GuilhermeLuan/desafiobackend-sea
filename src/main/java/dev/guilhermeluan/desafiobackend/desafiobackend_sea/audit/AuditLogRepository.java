package dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByResourceTypeAndResourceIdOrderByIdAsc(AuditResourceType resourceType, Long resourceId);
}
