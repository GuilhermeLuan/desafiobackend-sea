package dev.guilhermeluan.desafiobackend.desafiobackend_sea.exception;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.cep.CepServiceUnavailableException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.cep.InvalidCepException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage.CoverageAccessDeniedException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.InvalidSolicitationDataException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.InvalidSolicitationStateException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.SolicitationNotFoundException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client.IncompleteSolicitationException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.EmailAlreadyRegisteredException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.InvalidCoverageAssignmentException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.InvalidRoleException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // erros de domínio

    private static ResponseEntity<ApiError> respond(HttpStatus status, ErrorCode code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), code, message, request.getRequestURI()));
    }

    private static String pathOf(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest ? servletRequest.getRequest().getRequestURI() : null;
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException e, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, ErrorCode.EMAIL_ALREADY_REGISTERED, "Email already registered", request);
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<ApiError> handleInvalidRole(InvalidRoleException e, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_ROLE, e.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException e, HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS, "Invalid email or password", request);
    }

    @ExceptionHandler(InvalidCoverageAssignmentException.class)
    public ResponseEntity<ApiError> handleInvalidCoverage(InvalidCoverageAssignmentException e, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_COVERAGE, e.getMessage(), request);
    }

    @ExceptionHandler(CoverageAccessDeniedException.class)
    public ResponseEntity<ApiError> handleCoverageAccessDenied(CoverageAccessDeniedException e, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Resource not found", request);
    }

    @ExceptionHandler(SolicitationNotFoundException.class)
    public ResponseEntity<ApiError> handleSolicitationNotFound(SolicitationNotFoundException e, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Resource not found", request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException e, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "User not found", request);
    }

    @ExceptionHandler(InvalidSolicitationStateException.class)
    public ResponseEntity<ApiError> handleInvalidSolicitationState(InvalidSolicitationStateException e, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE, e.getMessage(), request);
    }

    @ExceptionHandler(InvalidSolicitationDataException.class)
    public ResponseEntity<ApiError> handleInvalidSolicitationData(InvalidSolicitationDataException e, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATA, e.getMessage(), request);
    }

    @ExceptionHandler(InvalidCepException.class)
    public ResponseEntity<ApiError> handleInvalidCep(InvalidCepException e, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_CEP, "CEP not found", request);
    }

    @ExceptionHandler(CepServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleCepServiceUnavailable(CepServiceUnavailableException e, HttpServletRequest request) {
        return respond(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.CEP_SERVICE_UNAVAILABLE,
                "CEP lookup is temporarily unavailable, try again later", request);
    }

    @ExceptionHandler(IncompleteSolicitationException.class)
    public ResponseEntity<ApiError> handleIncompleteSolicitation(IncompleteSolicitationException e, HttpServletRequest request) {
        ApiError error = ApiError.of(HttpStatus.BAD_REQUEST.value(), ErrorCode.INCOMPLETE_SOLICITATION,
                        "Solicitation is incomplete and cannot be submitted", request.getRequestURI())
                .withFieldErrors(e.getMissingFields().stream()
                        .map(field -> new ApiError.FieldError(field, "required"))
                        .toList())
                .withMissingSteps(e.getMissingSteps());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "Unexpected error", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        ApiError body = ApiError.of(status.value(), ErrorCode.VALIDATION_ERROR, "Validation failed", pathOf(request))
                .withFieldErrors(fieldErrors);
        return ResponseEntity.status(status).headers(headers).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ApiError body = ApiError.of(status.value(), ErrorCode.MALFORMED_REQUEST,
                "Request body is missing, malformed or has an invalid value", pathOf(request));
        return ResponseEntity.status(status).headers(headers).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        ErrorCode code = statusCode.value() == HttpStatus.NOT_FOUND.value() ? ErrorCode.NOT_FOUND : ErrorCode.REQUEST_ERROR;
        String message = status != null ? status.getReasonPhrase() : "Request error";
        ApiError error = ApiError.of(statusCode.value(), code, message, pathOf(request));
        return ResponseEntity.status(statusCode).headers(headers).body(error);
    }
}
