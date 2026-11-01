package dev.guilhermeluan.desafiobackend.desafiobackend_sea.search;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Solicitation;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.SolicitationChangedEvent;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.SolicitationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Component
@Slf4j
public class SolicitationIndexer {

    private final SolicitationRepository solicitationRepository;
    private final SolicitationSearchRepository searchRepository;

    public SolicitationIndexer(SolicitationRepository solicitationRepository, SolicitationSearchRepository searchRepository) {
        this.solicitationRepository = solicitationRepository;
        this.searchRepository = searchRepository;
    }


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSolicitationChanged(SolicitationChangedEvent event) {

        try {
            Optional<Solicitation> found = solicitationRepository.findById(event.solicitationId());
            if (found.isEmpty()) {
                return;
            }
            searchRepository.save(SolicitationDocument.from(found.get()));
        } catch (Exception e) {
            log.error("Failed to index solicitation {} in Elasticsearch. The database change was already committed, "
                      + "so the search index may be stale until this solicitation changes again", event.solicitationId(), e);
        }

    }
}
