package dev.guilhermeluan.desafiobackend.desafiobackend_sea.search;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Solicitation;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.SolicitationChangedEvent;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.SolicitationRepository;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitationIndexerTest {

    private static final Long ID = 7L;
    private static final SolicitationChangedEvent EVENT = new SolicitationChangedEvent(ID);

    @Mock
    private SolicitationRepository solicitationRepository;
    @Mock
    private SolicitationSearchRepository searchRepository;

    @InjectMocks
    private SolicitationIndexer indexer;

    private static Solicitation draftWithId() {
        Solicitation solicitation = Solicitation.draft(10L);
        org.springframework.test.util.ReflectionTestUtils.setField(solicitation, "id", ID);
        return solicitation;
    }

    @Test
    void theCurrentStateFromTheDatabaseIsWhatGoesToTheIndex() {
        when(solicitationRepository.findById(ID)).thenReturn(Optional.of(draftWithId()));

        indexer.onSolicitationChanged(EVENT);

        ArgumentCaptor<SolicitationDocument> saved = ArgumentCaptor.forClass(SolicitationDocument.class);
        verify(searchRepository).save(saved.capture());
        assertEquals(ID, saved.getValue().getId());
        assertEquals(Status.DRAFT, saved.getValue().getStatus());
        assertEquals(10L, saved.getValue().getOwnerId());
    }

    @Test
    void aSolicitationThatNoLongerExistsIsIgnored() {
        when(solicitationRepository.findById(ID)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> indexer.onSolicitationChanged(EVENT));

        verify(searchRepository, never()).save(any());
    }

    @Test
    void anElasticsearchFailureNeverReachesTheCaller() {
        when(solicitationRepository.findById(ID)).thenReturn(Optional.of(draftWithId()));
        doThrow(new RuntimeException("Elasticsearch is down")).when(searchRepository).save(any());

        assertDoesNotThrow(() -> indexer.onSolicitationChanged(EVENT));
    }

    @Test
    void aDatabaseFailureWhileReadingNeverReachesTheCaller() {
        when(solicitationRepository.findById(ID)).thenThrow(new RuntimeException("database is down"));

        assertDoesNotThrow(() -> indexer.onSolicitationChanged(EVENT));

        verify(searchRepository, never()).save(any());
    }

    @Test
    void theListenerRunsOnlyAfterTheTransactionCommits() throws NoSuchMethodException {
        TransactionalEventListener listener = SolicitationIndexer.class
                .getMethod("onSolicitationChanged", SolicitationChangedEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertNotNull(listener, "o método precisa ser um @TransactionalEventListener");
        assertEquals(TransactionPhase.AFTER_COMMIT, listener.phase());
    }
}
