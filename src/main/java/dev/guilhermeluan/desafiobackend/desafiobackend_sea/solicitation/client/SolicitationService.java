package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit.Audit;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit.AuditAction;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.cep.CepClient;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.cep.CepResult;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.cep.InvalidCepException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.*;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client.dtos.AddressRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client.dtos.BasicDataRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client.dtos.ConfirmationRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service
public class SolicitationService {

    private final SolicitationRepository solicitationRepository;
    private final CepClient cepClient;
    private final ApplicationEventPublisher events;

    public SolicitationService(SolicitationRepository solicitationRepository, CepClient cepClient,
                               ApplicationEventPublisher events) {
        this.solicitationRepository = solicitationRepository;
        this.cepClient = cepClient;
        this.events = events;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Transactional
    public Solicitation createDraft(Long ownerId) {
        Solicitation draft = Solicitation.draft(ownerId);
        Solicitation savedDraft = solicitationRepository.save(draft);

        events.publishEvent(new SolicitationChangedEvent(savedDraft.getId()));

        return savedDraft;
    }

    @Transactional(readOnly = true)
    public Solicitation get(Long ownerId, Long id) {
        return findOwned(ownerId, id);
    }

    @Transactional
    public Solicitation saveBasicData(Long ownerId, Long id, BasicDataRequest request) {
        Solicitation draft = findOwned(ownerId, id);
        draft.saveBasicData(request.serviceType(), request.title(), request.description());

        events.publishEvent(new SolicitationChangedEvent(id));

        return draft;
    }

    @Transactional
    public Solicitation saveAddress(Long ownerId, Long id, AddressRequest request) {
        Solicitation draft = findOwned(ownerId, id);

        CepResult cep = cepClient.findByCep(request.cep())
                .orElseThrow(() -> new InvalidCepException(request.cep()));

        draft.saveAddress(new Address(request.cep(), cep.street(), cep.neighborhood(), cep.city(), cep.state(),
                request.number(), request.complement()));

        events.publishEvent(new SolicitationChangedEvent(id));

        return draft;
    }

    @Transactional
    public Solicitation saveConfirmation(Long ownerId, Long id, ConfirmationRequest request) {
        Solicitation draft = findOwned(ownerId, id);
        draft.saveConfirmation(request.priority(), request.preferredDate(), request.estimatedValue(), request.termsAccepted());

        events.publishEvent(new SolicitationChangedEvent(id));

        return draft;
    }

    private Solicitation findOwned(Long ownerId, Long draftId) {

        Solicitation draft = solicitationRepository.findById(draftId).orElseThrow(() -> new SolicitationNotFoundException(draftId));
        if (!Objects.equals(draft.getOwnerId(), ownerId)) {
            throw new SolicitationNotFoundException(draftId);
        }
        return draft;

    }

    @Transactional
    @Audit(action = AuditAction.SUBMIT, resourceId = "#id")
    public Solicitation submit(Long ownerId, Long id) {
        Solicitation draft = findOwned(ownerId, id);

        Map<Integer, List<String>> missingByStep = new LinkedHashMap<>();
        missingByStep.put(1, missingInStep1(draft));
        missingByStep.put(2, missingInStep2(draft));
        missingByStep.put(3, missingInStep3(draft));

        List<Integer> missingSteps = new ArrayList<>();
        List<String> missingFields = new ArrayList<>();
        missingByStep.forEach((step, fields) -> {
            if (!fields.isEmpty()) {
                missingSteps.add(step);
                missingFields.addAll(fields);
            }
        });

        if (!missingSteps.isEmpty()) {
            throw new IncompleteSolicitationException(missingSteps, missingFields);
        }
        draft.submit();

        events.publishEvent(new SolicitationChangedEvent(id));

        return draft;
    }

    private List<String> missingInStep1(Solicitation s) {
        List<String> missing = new ArrayList<>();
        if (s.getServiceType() == null) missing.add("serviceType");
        if (isBlank(s.getTitle())) missing.add("title");
        if (isBlank(s.getDescription())) missing.add("description");
        return missing;
    }

    private List<String> missingInStep2(Solicitation s) {
        List<String> missing = new ArrayList<>();
        if (isBlank(s.getCep())) missing.add("cep");
        if (isBlank(s.getNumber())) missing.add("number");
        return missing;
    }

    private List<String> missingInStep3(Solicitation s) {
        List<String> missing = new ArrayList<>();
        if (s.getPriority() == null) missing.add("priority");
        if (s.getEstimatedValue() == null) missing.add("estimatedValue");
        if (!s.isTermsAccepted()) missing.add("termsAccepted");
        return missing;
    }
}

