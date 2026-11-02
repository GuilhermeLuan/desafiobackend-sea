package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.analyst;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit.Audit;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit.AuditAction;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage.CoverageGuard;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.*;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalystSolicitationService {

    private final SolicitationRepository solicitationRepository;
    private final CoverageGuard coverageGuard;
    private final ApplicationEventPublisher events;

    public AnalystSolicitationService(SolicitationRepository solicitationRepository, CoverageGuard coverageGuard,
                                      ApplicationEventPublisher events) {
        this.solicitationRepository = solicitationRepository;
        this.coverageGuard = coverageGuard;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Solicitation get(String analystEmail, Long id) {
        return findVisible(analystEmail, id);
    }

    @Transactional
    @Audit(action = AuditAction.START, resourceId = "#id")
    public Solicitation start(String analystEmail, Long id) {
        Solicitation solicitation = findVisible(analystEmail, id);

        solicitation.start();

        events.publishEvent(new SolicitationChangedEvent(id));

        return solicitation;
    }

    @Transactional
    @Audit(action = AuditAction.DECIDE, resourceId = "#id")
    public Solicitation decide(String analystEmail, Long analystId, Long id, Decision decision, String comment) {
        Solicitation solicitation = findVisible(analystEmail, id);

        solicitation.decide(decision, analystId, comment);

        events.publishEvent(new SolicitationChangedEvent(id));

        return solicitation;
    }

    private Solicitation findVisible(String email, Long id) {
        Solicitation solicitation = solicitationRepository.findById(id).orElseThrow(() -> new SolicitationNotFoundException(id));

        if (solicitation.getStatus() == Status.DRAFT) {
            throw new SolicitationNotFoundException(id);
        }

        coverageGuard.assertCovers(email, Role.ANALYST, solicitation.getState());

        return solicitation;
    }
}
