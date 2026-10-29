package dev.guilhermeluan.desafiobackend.desafiobackend_sea.users;

import org.springframework.dao.DataIntegrityViolationException;
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

    public User createPrivilegedUser(String email, String password, Role role, Set<String> coverageUfs) {
        Set<String> ufs = coverageUfs == null ? Set.of() : coverageUfs;

        switch (role) {
            case ANALYST -> {
                if (ufs.isEmpty()) {
                    throw new InvalidCoverageException("Analyst users require at least one coverage UF");
                }
            }
            case ADMIN -> {
                if (!ufs.isEmpty()) {
                    throw new InvalidCoverageException("Admin users must not have coverage UFs");
                }
            }
            default -> throw new InvalidRoleException(role);
        }

        return createUser(email, password, role, ufs);
    }

    private void throwIfEmailAlreadyRegistered(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
    }
}
