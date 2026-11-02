package dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(nullable = false, updatable = false)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AuditResourceType resourceType;

    @Column(updatable = false)
    private Long resourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AuditOutcome outcome;

    @Column(updatable = false)
    private String errorType;

    protected AuditLog() {
    }

    public AuditLog(String actor, AuditAction action, Long resourceId, AuditOutcome outcome, String errorType) {
        this.actor = actor;
        this.action = action;
        this.resourceType = action.resourceType();
        this.resourceId = resourceId;
        this.outcome = outcome;
        this.errorType = errorType;
    }
}
