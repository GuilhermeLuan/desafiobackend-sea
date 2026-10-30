package dev.guilhermeluan.desafiobackend.desafiobackend_sea.exception;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage.CoverageAccessDeniedException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.EmailAlreadyRegisteredException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.InvalidCoverageAssignmentException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.InvalidRoleException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ProblemDetail handleEmailAlreadyRegistered(EmailAlreadyRegisteredException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Email already registered");
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ProblemDetail handleInvalidRole(InvalidRoleException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(InvalidCoverageAssignmentException.class)
    public ProblemDetail handleInvalidCoverage(InvalidCoverageAssignmentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(CoverageAccessDeniedException.class)
    public ProblemDetail handleCoverageAccessDenied(CoverageAccessDeniedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "User not found");
    }
}
