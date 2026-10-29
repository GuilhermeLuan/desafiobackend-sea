package dev.guilhermeluan.desafiobackend.desafiobackend_sea.users;

public class InvalidRoleException extends RuntimeException {

    public InvalidRoleException(Role role) {
        super("Role not allowed for this operation: " + role);
    }
}
