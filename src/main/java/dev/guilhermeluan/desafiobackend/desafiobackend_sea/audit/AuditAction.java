package dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit;

public enum AuditAction {
    REGISTER(AuditResourceType.USER),
    CREATE_USER(AuditResourceType.USER),
    SUBMIT(AuditResourceType.SOLICITATION),
    START(AuditResourceType.SOLICITATION),
    DECIDE(AuditResourceType.SOLICITATION);

    private final AuditResourceType resourceType;

    AuditAction(AuditResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public AuditResourceType resourceType() {
        return resourceType;
    }
}
