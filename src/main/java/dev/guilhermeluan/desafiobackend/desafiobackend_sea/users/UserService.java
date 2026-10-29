package dev.guilhermeluan.desafiobackend.desafiobackend_sea.users;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public UserService(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    public User createUser(String email, String password, Role role) {
        throwIfEmailAlreadyRegistered(email);
        
        User user = new User(null, email, encoder.encode(password), role);
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyRegisteredException(email);
        }

        return user;
    }

    public User createPrivilegedUser(String email, String password, Role role) {
        if (role != Role.ANALYST && role != Role.ADMIN) {
            throw new InvalidRoleException(role);
        }

        return createUser(email, password, role);
    }

    private void throwIfEmailAlreadyRegistered(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
    }
}
