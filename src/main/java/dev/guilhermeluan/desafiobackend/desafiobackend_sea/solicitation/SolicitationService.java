package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.dtos.AddressRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.dtos.BasicDataRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.dtos.ConfirmationRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
public class SolicitationService {

    private final SolicitationRepository solicitationRepository;
    private final CepClient cepClient;

    public SolicitationService(SolicitationRepository solicitationRepository, CepClient cepClient) {
        this.solicitationRepository = solicitationRepository;
        this.cepClient = cepClient;
    }

    @Transactional
    public Solicitation createDraft(Long ownerId) {
        Solicitation draft = Solicitation.draft(ownerId);
        return solicitationRepository.save(draft);
    }

    @Transactional(readOnly = true)
    public Solicitation get(Long ownerId, Long id) {
        return findOwned(ownerId, id);
    }

    @Transactional
    public Solicitation saveBasicData(Long ownerId, Long id, BasicDataRequest request) {
        Solicitation draft = findOwned(ownerId, id);
        draft.saveBasicData(request.serviceType(), request.title(), request.description());
        return draft;
    }

    @Transactional
    public Solicitation saveAddress(Long ownerId, Long id, AddressRequest request) {
        Solicitation draft = findOwned(ownerId, id);

        CepResult cep = cepClient.findByCep(request.cep())
                .orElseThrow(() -> new InvalidCepException(request.cep()));

        draft.saveAddress(new Address(request.cep(), cep.street(), cep.neighborhood(), cep.city(), cep.state(),
                request.number(), request.complement()));
        return draft;
    }

    @Transactional
    public Solicitation saveConfirmation(Long ownerId, Long id, ConfirmationRequest request) {
        Solicitation draft = findOwned(ownerId, id);
        draft.saveConfirmation(request.priority(), request.preferredDate(), request.estimatedValue(), request.termsAccepted());
        return draft;
    }


    private Solicitation findOwned(Long ownerId, Long draftId) {

        Solicitation draft = solicitationRepository.findById(draftId).orElseThrow(() -> new SolicitationNotFoundException(draftId));
        if(!Objects.equals(draft.getOwnerId(), ownerId)){
            throw new SolicitationNotFoundException(draftId);
        }
        return draft;

    }
}

