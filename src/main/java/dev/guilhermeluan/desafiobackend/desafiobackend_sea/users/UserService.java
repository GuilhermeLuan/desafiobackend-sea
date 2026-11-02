package dev.guilhermeluan.desafiobackend.desafiobackend_sea.users;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit.Audit;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit.AuditAction;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public UserService(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    public User createUser(String email, String password, Role role) {
        return createUser(email, password, role, Set.of());
    }

    private User createUser(String email, String password, Role role, Set<String> coverageUfs) {
        throwIfEmailAlreadyRegistered(email);

        User user = new User(null, email, encoder.encode(password), role);
        user.setCoverageUfs(new HashSet<>(coverageUfs));

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyRegisteredException(email);
        }

        return user;
    }

    @Audit(action = AuditAction.CREATE_USER, resourceId = "#result.id")
    public User createPrivilegedUser(String email, String password, Role role, Set<String> coverageUfs) {
        if (role != Role.ANALYST && role != Role.ADMIN) {
            throw new InvalidRoleException(role);
        }

        Set<String> ufs = orEmpty(coverageUfs);
        validateCoverage(role, ufs);

        return createUser(email, password, role, ufs);
    }

    private void throwIfEmailAlreadyRegistered(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
    }

    public User updateCoverage(long id, Set<String> coverageUfs) {
        User user = findByIdOrThrowUserNotFoundException(id);
        Set<String> ufs = orEmpty(coverageUfs);
        validateCoverage(user.getRole(), ufs);

        user.setCoverageUfs(new HashSet<>(ufs));
        return userRepository.saveAndFlush(user);
    }

    private User findByIdOrThrowUserNotFoundException(long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    public Long findIdByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new BadCredentialsException("User not found for the authenticated email");
        }
        return user.getId();
    }

    private static void validateCoverage(Role role, Set<String> ufs) {
        switch (role) {
            case ANALYST -> {
                if (ufs.isEmpty()) {
                    throw new InvalidCoverageAssignmentException("Analyst users require at least one coverage UF");
                }
            }
            case ADMIN -> {
                if (!ufs.isEmpty()) {
                    throw new InvalidCoverageAssignmentException("Admin users must not have coverage UFs");
                }
            }
            case CLIENT -> throw new InvalidCoverageAssignmentException("Client users do not have coverage UFs");
        }
    }

    private static Set<String> orEmpty(Set<String> ufs) {
        return ufs == null ? Set.of() : ufs;
    }
}
