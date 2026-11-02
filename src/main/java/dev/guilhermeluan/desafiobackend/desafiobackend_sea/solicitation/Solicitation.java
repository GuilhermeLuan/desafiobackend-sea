package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;

@Getter
@Entity
@Table(name = "tb_solicitations")
public class Solicitation {

    private static final int LAST_STEP = 3;
    private static final BigDecimal HIGH_PRIORITY_MIN_VALUE = BigDecimal.valueOf(100);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private Long decidedBy;
    private String decisionComment;

    @Column(nullable = false)
    private int currentStep;

    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;

    private String title;
    private String description;

    private String cep;
    private String street;
    private String neighborhood;
    private String city;

    @Column(length = 2, columnDefinition = "char(2)")
    private String state;

    private String number;
    private String complement;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    private LocalDate preferredDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal estimatedValue;

    @Column(nullable = false)
    private boolean termsAccepted;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    private Instant submittedAt;

    protected Solicitation() {
    }

    private Solicitation(Long ownerId) {
        this.ownerId = ownerId;
        this.status = Status.DRAFT;
        this.currentStep = 1;
    }

    public static Solicitation draft(Long ownerId) {
        return new Solicitation(ownerId);
    }

    public void submit() {
        requireStatus(Status.DRAFT);
        this.status = Status.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    public void start() {
        requireStatus(Status.SUBMITTED);
        this.status = Status.IN_REVIEW;
    }

    public void decide(Decision decision, Long analystId, String comment) {
        requireStatus(Status.SUBMITTED, Status.IN_REVIEW);

        Objects.requireNonNull(analystId, "analystId must not be null");
        Objects.requireNonNull(comment, "comment must not be null");
        Objects.requireNonNull(decision, "decision must not be null");


        switch (decision) {
            case APPROVE:
                this.status = Status.APPROVED;
                break;
            case REJECT:
                this.status = Status.REJECTED;
                break;
        }

        this.decidedBy = analystId;
        this.decisionComment = comment;
    }

    public void assertEditable() {
        requireStatus(Status.DRAFT);
    }

    private void requireStatus(Status... allowed) {
        if (!Arrays.asList(allowed).contains(status)) {
            throw new InvalidSolicitationStateException(
                    "Operation not allowed in status " + status + " (allowed: " + Arrays.toString(allowed) + ")");
        }
    }

    public void saveBasicData(ServiceType serviceType, String title, String description) {
        assertEditable();

        this.serviceType = serviceType;
        this.title = title != null ? title.trim() : null;
        this.description = description != null ? description.trim() : null;
        advanceTo(2);
    }

    public void saveAddress(Address address) {
        assertEditable();
        Objects.requireNonNull(address, "address must not be null");

        if (currentStep < 2) {
            throw new InvalidSolicitationStateException("Step 1 must be saved before the address");
        }

        this.cep = address.cep();
        this.street = address.street();
        this.neighborhood = address.neighborhood();
        this.city = address.city();
        this.state = address.state();
        this.number = address.number();
        this.complement = address.complement();

        advanceTo(3);
    }

    public void saveConfirmation(Priority priority, LocalDate preferredDate, BigDecimal estimatedValue, boolean termsAccepted) {
        assertEditable();
        Objects.requireNonNull(priority, "priority must not be null");

        if (currentStep < 3) {
            throw new InvalidSolicitationStateException("Step 2 must be saved before the confirmation");
        }

        requireValueForHighPriority(priority, estimatedValue);

        this.priority = priority;
        this.preferredDate = preferredDate;
        this.estimatedValue = estimatedValue;
        this.termsAccepted = termsAccepted;

        advanceTo(LAST_STEP);
    }

    public void assertSubmittable(LocalDate today) {
        assertEditable();

        if (preferredDate != null && preferredDate.isBefore(today)) {
            throw new InvalidSolicitationDataException("Preferred date is in the past; save step 3 again before submitting");
        }
        requireValueForHighPriority(priority, estimatedValue);
    }

    private static void requireValueForHighPriority(Priority priority, BigDecimal estimatedValue) {
        if (priority == Priority.HIGH
            && (estimatedValue == null || estimatedValue.compareTo(HIGH_PRIORITY_MIN_VALUE) < 0)) {
            throw new InvalidSolicitationDataException("High priority requires an estimated value of at least 100");
        }
    }

    private void advanceTo(int step) {
        this.currentStep = Math.max(this.currentStep, Math.min(step, LAST_STEP));
    }
}
