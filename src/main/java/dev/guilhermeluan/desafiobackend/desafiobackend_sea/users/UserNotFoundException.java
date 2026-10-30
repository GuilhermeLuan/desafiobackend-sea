package dev.guilhermeluan.desafiobackend.desafiobackend_sea.users;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("User not found: " + id);
    }
}
