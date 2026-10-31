package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.analyst;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage.CoverageGuard;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.*;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalystSolicitationService {

    private final SolicitationRepository solicitationRepository;
    private final CoverageGuard coverageGuard;

    public AnalystSolicitationService(SolicitationRepository solicitationRepository, CoverageGuard coverageGuard) {
        this.solicitationRepository = solicitationRepository;
        this.coverageGuard = coverageGuard;
    }

    @Transactional(readOnly = true)
    public Solicitation get(String analystEmail, Long id) {
        return findVisible(analystEmail, id);
    }

    @Transactional
    public Solicitation start(String analystEmail, Long id) {
        Solicitation solicitation = findVisible(analystEmail, id);

        solicitation.start();

        return solicitation;
    }

    @Transactional
    public Solicitation decide(String analystEmail, Long analystId, Long id, Decision decision, String comment) {
        Solicitation solicitation = findVisible(analystEmail, id);

        solicitation.decide(decision, analystId, comment);

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
