package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.cep.CepClient;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.cep.CepResult;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.cep.CepServiceUnavailableException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.cep.InvalidCepException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.*;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client.dtos.AddressRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client.dtos.BasicDataRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client.dtos.ConfirmationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitationServiceTest {

    private static final Long OWNER_ID = 10L;
    private static final Long OTHER_CLIENT_ID = 11L;
    private static final Long ID = 1L;
    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);

    private static final CepResult NINHO_DO_URUBU =
            new CepResult("Estrada dos Bandeirantes", "Vargem Grande", "Rio de Janeiro", "RJ");

    private static final BasicDataRequest BASIC =
            new BasicDataRequest(ServiceType.INSTALLATION, "Instalar ar-condicionado", "Instalação de um split na sala de estar");
    private static final AddressRequest ADDRESS = new AddressRequest("22785-275", "25997", "CT George Helal");

    @Mock
    private SolicitationRepository solicitationRepository;
    @Mock
    private CepClient cepClient;
    private static final SolicitationChangedEvent CHANGED = new SolicitationChangedEvent(ID);

    @InjectMocks
    private SolicitationService service;

    private Solicitation draft;

    private static Solicitation persisted(Solicitation solicitation) {
        ReflectionTestUtils.setField(solicitation, "id", ID);
        return solicitation;
    }

    @BeforeEach
    void setUp() {
        draft = persisted(Solicitation.draft(OWNER_ID));
        lenient().when(solicitationRepository.findById(ID)).thenReturn(Optional.of(draft));
    }

    private void withStepOneSaved() {
        draft.saveBasicData(ServiceType.INSTALLATION, "Instalar ar-condicionado", "Instalação de um split na sala de estar");
    }

    private void withStepTwoSaved() {
        withStepOneSaved();
        draft.saveAddress(new Address("22785275", "Estrada dos Bandeirantes", "Vargem Grande", "Rio de Janeiro", "RJ", "25997", null));
    }

    private void withStepThreeSaved() {
        withStepTwoSaved();
        draft.saveConfirmation(Priority.MEDIUM, TOMORROW, new BigDecimal("250.00"), true);
    }

    // createDraft

    @Test
    void createDraftSavesADraftOwnedByTheClient() {
        when(solicitationRepository.save(any(Solicitation.class))).thenAnswer(i -> i.getArgument(0));

        Solicitation created = service.createDraft(OWNER_ID);

        assertEquals(OWNER_ID, created.getOwnerId());
        assertEquals(Status.DRAFT, created.getStatus());
        assertEquals(1, created.getCurrentStep());
    }

    // get

    @Test
    void getReturnsTheSolicitationToItsOwner() {
        assertEquals(draft, service.get(OWNER_ID, ID));
    }

    @Test
    void getHidesASolicitationOwnedBySomeoneElse() {
        assertThrows(SolicitationNotFoundException.class, () -> service.get(OTHER_CLIENT_ID, ID));
    }

    @Test
    void getFailsForAnIdThatDoesNotExist() {
        when(solicitationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SolicitationNotFoundException.class, () -> service.get(OWNER_ID, 99L));
    }

    // etapa 1

    @Test
    void saveBasicDataStoresTheFieldsAndMovesToStepTwo() {
        Solicitation result = service.saveBasicData(OWNER_ID, ID, BASIC);

        assertEquals(ServiceType.INSTALLATION, result.getServiceType());
        assertEquals("Instalar ar-condicionado", result.getTitle());
        assertEquals(2, result.getCurrentStep());
    }

    @Test
    void saveBasicDataIsDeniedToAnotherClientAndChangesNothing() {
        assertThrows(SolicitationNotFoundException.class, () -> service.saveBasicData(OTHER_CLIENT_ID, ID, BASIC));

        assertNull(draft.getTitle());
        assertEquals(1, draft.getCurrentStep());
    }

    @Test
    void saveBasicDataFailsForAnIdThatDoesNotExist() {
        when(solicitationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SolicitationNotFoundException.class, () -> service.saveBasicData(OWNER_ID, 99L, BASIC));
    }

    @Test
    void saveBasicDataIsRejectedOnceSubmitted() {
        withStepTwoSaved();
        draft.saveConfirmation(Priority.LOW, TOMORROW, BigDecimal.TEN, true);
        draft.submit();

        assertThrows(InvalidSolicitationStateException.class, () -> service.saveBasicData(OWNER_ID, ID, BASIC));
    }

    // etapa 2

    @Test
    void saveAddressFillsStreetNeighborhoodCityAndUfFromTheCepLookup() {
        withStepOneSaved();
        when(cepClient.findByCep("22785275")).thenReturn(Optional.of(NINHO_DO_URUBU));

        Solicitation result = service.saveAddress(OWNER_ID, ID, ADDRESS);

        assertEquals("22785275", result.getCep());
        assertEquals("Estrada dos Bandeirantes", result.getStreet());
        assertEquals("Vargem Grande", result.getNeighborhood());
        assertEquals("Rio de Janeiro", result.getCity());
        assertEquals("RJ", result.getState());
        assertEquals("25997", result.getNumber());
        assertEquals("CT George Helal", result.getComplement());
        assertEquals(3, result.getCurrentStep());
    }

    @Test
    void saveAddressWithUnknownCepIsRejectedAndKeepsTheStep() {
        withStepOneSaved();
        when(cepClient.findByCep("22785275")).thenReturn(Optional.empty());

        assertThrows(InvalidCepException.class, () -> service.saveAddress(OWNER_ID, ID, ADDRESS));

        assertNull(draft.getCep());
        assertNull(draft.getState());
        assertEquals(2, draft.getCurrentStep());
    }

    @Test
    void saveAddressWhenTheCepServiceIsDownKeepsTheStep() {
        withStepOneSaved();
        when(cepClient.findByCep("22785275"))
                .thenThrow(new CepServiceUnavailableException("ViaCEP is down", new RuntimeException()));

        assertThrows(CepServiceUnavailableException.class, () -> service.saveAddress(OWNER_ID, ID, ADDRESS));

        assertNull(draft.getCep());
        assertNull(draft.getState());
        assertEquals(2, draft.getCurrentStep());
    }

    @Test
    void saveAddressChecksOwnershipBeforeCallingTheCepService() {
        withStepOneSaved();

        assertThrows(SolicitationNotFoundException.class, () -> service.saveAddress(OTHER_CLIENT_ID, ID, ADDRESS));

        verifyNoInteractions(cepClient);
        assertNull(draft.getCep());
    }

    @Test
    void saveAddressRequiresStepOneFirst() {
        lenient().when(cepClient.findByCep("22785275")).thenReturn(Optional.of(NINHO_DO_URUBU));

        assertThrows(InvalidSolicitationStateException.class, () -> service.saveAddress(OWNER_ID, ID, ADDRESS));

        assertNull(draft.getCep());
        assertEquals(1, draft.getCurrentStep());
    }

    // etapa 3

    @Test
    void saveConfirmationStoresTheFields() {
        withStepTwoSaved();

        Solicitation result = service.saveConfirmation(OWNER_ID, ID,
                new ConfirmationRequest(Priority.MEDIUM, TOMORROW, new BigDecimal("250.00"), true));

        assertEquals(Priority.MEDIUM, result.getPriority());
        assertEquals(TOMORROW, result.getPreferredDate());
        assertEquals(new BigDecimal("250.00"), result.getEstimatedValue());
        assertEquals(3, result.getCurrentStep());
    }

    @Test
    void saveConfirmationIsDeniedToAnotherClientAndChangesNothing() {
        withStepTwoSaved();

        assertThrows(SolicitationNotFoundException.class, () -> service.saveConfirmation(OTHER_CLIENT_ID, ID,
                new ConfirmationRequest(Priority.LOW, TOMORROW, BigDecimal.TEN, true)));

        assertNull(draft.getPriority());
    }

    @Test
    void saveConfirmationAppliesTheHighPriorityRule() {
        withStepTwoSaved();

        assertThrows(InvalidSolicitationDataException.class, () -> service.saveConfirmation(OWNER_ID, ID,
                new ConfirmationRequest(Priority.HIGH, TOMORROW, new BigDecimal("99.99"), true)));

        assertNull(draft.getPriority());
    }

    @Test
    void saveConfirmationRequiresStepTwoFirst() {
        withStepOneSaved();

        assertThrows(InvalidSolicitationStateException.class, () -> service.saveConfirmation(OWNER_ID, ID,
                new ConfirmationRequest(Priority.LOW, TOMORROW, BigDecimal.TEN, true)));
    }

    // submit

    @Test
    void submitMovesACompleteDraftToSubmitted() {
        withStepThreeSaved();

        Solicitation result = service.submit(OWNER_ID, ID);

        assertEquals(Status.SUBMITTED, result.getStatus());
        assertNotNull(result.getSubmittedAt());
    }

    @Test
    void submitDoesNotRequireAPreferredDate() {
        withStepTwoSaved();
        draft.saveConfirmation(Priority.LOW, null, BigDecimal.TEN, true);

        assertEquals(Status.SUBMITTED, service.submit(OWNER_ID, ID).getStatus());
    }

    @Test
    void submitIsDeniedToAnotherClientAndChangesNothing() {
        withStepThreeSaved();

        assertThrows(SolicitationNotFoundException.class, () -> service.submit(OTHER_CLIENT_ID, ID));

        assertEquals(Status.DRAFT, draft.getStatus());
        assertNull(draft.getSubmittedAt());
    }

    @Test
    void submitFailsForAnIdThatDoesNotExist() {
        when(solicitationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SolicitationNotFoundException.class, () -> service.submit(OWNER_ID, 99L));
    }

    @Test
    void submitOfAnEmptyDraftListsAllThreeStepsAndTheirFields() {
        IncompleteSolicitationException e =
                assertThrows(IncompleteSolicitationException.class, () -> service.submit(OWNER_ID, ID));

        assertEquals(List.of(1, 2, 3), e.getMissingSteps());
        assertEquals(Set.of("serviceType", "title", "description", "cep", "number",
                "priority", "estimatedValue", "termsAccepted"), Set.copyOf(e.getMissingFields()));
        assertEquals(Status.DRAFT, draft.getStatus());
    }

    @Test
    void submitWithOnlyStepOneListsStepsTwoAndThree() {
        withStepOneSaved();

        IncompleteSolicitationException e =
                assertThrows(IncompleteSolicitationException.class, () -> service.submit(OWNER_ID, ID));

        assertEquals(List.of(2, 3), e.getMissingSteps());
        assertEquals(Set.of("cep", "number", "priority", "estimatedValue", "termsAccepted"),
                Set.copyOf(e.getMissingFields()));
    }

    @Test
    void submitWithOnlyStepsOneAndTwoListsStepThree() {
        withStepTwoSaved();

        IncompleteSolicitationException e =
                assertThrows(IncompleteSolicitationException.class, () -> service.submit(OWNER_ID, ID));

        assertEquals(List.of(3), e.getMissingSteps());
        assertEquals(Set.of("priority", "estimatedValue", "termsAccepted"), Set.copyOf(e.getMissingFields()));
        assertEquals(Status.DRAFT, draft.getStatus());
    }

    @Test
    void submitWithoutAcceptedTermsIsIncomplete() {
        withStepTwoSaved();
        draft.saveConfirmation(Priority.LOW, TOMORROW, BigDecimal.TEN, false);

        IncompleteSolicitationException e =
                assertThrows(IncompleteSolicitationException.class, () -> service.submit(OWNER_ID, ID));

        assertEquals(List.of(3), e.getMissingSteps());
        assertEquals(List.of("termsAccepted"), e.getMissingFields());
    }

    @Test
    void submitWithoutEstimatedValueIsIncomplete() {
        withStepTwoSaved();
        draft.saveConfirmation(Priority.LOW, TOMORROW, null, true);

        IncompleteSolicitationException e =
                assertThrows(IncompleteSolicitationException.class, () -> service.submit(OWNER_ID, ID));

        assertEquals(List.of(3), e.getMissingSteps());
        assertEquals(List.of("estimatedValue"), e.getMissingFields());
    }

    @Test
    void submitTwiceIsRejected() {
        withStepThreeSaved();
        service.submit(OWNER_ID, ID);

        assertThrows(InvalidSolicitationStateException.class, () -> service.submit(OWNER_ID, ID));
        assertEquals(Status.SUBMITTED, draft.getStatus());
    }

    // eventos para o índice de busca
    @Mock
    private ApplicationEventPublisher events;

    @Test
    void createDraftPublishesAChangedEventWithTheGeneratedId() {
        when(solicitationRepository.save(any(Solicitation.class))).thenAnswer(i -> persisted(i.getArgument(0)));

        service.createDraft(OWNER_ID);

        verify(events).publishEvent(CHANGED);
    }

    @Test
    void saveBasicDataPublishesAChangedEvent() {
        service.saveBasicData(OWNER_ID, ID, BASIC);

        verify(events).publishEvent(CHANGED);
    }

    @Test
    void saveAddressPublishesAChangedEvent() {
        withStepOneSaved();
        when(cepClient.findByCep("22785275")).thenReturn(Optional.of(NINHO_DO_URUBU));

        service.saveAddress(OWNER_ID, ID, ADDRESS);

        verify(events).publishEvent(CHANGED);
    }

    @Test
    void saveConfirmationPublishesAChangedEvent() {
        withStepTwoSaved();

        service.saveConfirmation(OWNER_ID, ID, new ConfirmationRequest(Priority.MEDIUM, TOMORROW, new BigDecimal("250.00"), true));

        verify(events).publishEvent(CHANGED);
    }

    @Test
    void submitPublishesAChangedEvent() {
        withStepThreeSaved();

        service.submit(OWNER_ID, ID);

        verify(events).publishEvent(CHANGED);
    }

    @Test
    void nothingIsPublishedWhenTheClientIsNotTheOwner() {
        withStepThreeSaved();

        assertThrows(SolicitationNotFoundException.class, () -> service.saveBasicData(OTHER_CLIENT_ID, ID, BASIC));
        assertThrows(SolicitationNotFoundException.class, () -> service.submit(OTHER_CLIENT_ID, ID));

        verifyNoInteractions(events);
    }

    @Test
    void nothingIsPublishedWhenTheCepIsUnknownOrTheCepServiceIsDown() {
        withStepOneSaved();
        when(cepClient.findByCep("22785275")).thenReturn(Optional.empty());

        assertThrows(InvalidCepException.class, () -> service.saveAddress(OWNER_ID, ID, ADDRESS));

        when(cepClient.findByCep("22785275"))
                .thenThrow(new CepServiceUnavailableException("ViaCEP is down", new RuntimeException()));

        assertThrows(CepServiceUnavailableException.class, () -> service.saveAddress(OWNER_ID, ID, ADDRESS));

        verifyNoInteractions(events);
    }

    @Test
    void nothingIsPublishedWhenTheDomainRejectsTheChange() {
        withStepTwoSaved();

        assertThrows(InvalidSolicitationDataException.class, () -> service.saveConfirmation(OWNER_ID, ID,
                new ConfirmationRequest(Priority.HIGH, TOMORROW, new BigDecimal("99.99"), true)));

        verifyNoInteractions(events);
    }

    @Test
    void nothingIsPublishedWhenTheSubmitIsIncomplete() {
        withStepOneSaved();

        assertThrows(IncompleteSolicitationException.class, () -> service.submit(OWNER_ID, ID));

        verifyNoInteractions(events);
    }

    @Test
    void readingDoesNotPublishAnything() {
        service.get(OWNER_ID, ID);

        verifyNoInteractions(events);
    }
}
