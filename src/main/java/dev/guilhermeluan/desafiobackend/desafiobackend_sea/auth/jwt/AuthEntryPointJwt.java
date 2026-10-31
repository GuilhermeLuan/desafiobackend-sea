package dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.jwt;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.exception.ErrorCode;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.exception.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private final ErrorResponseWriter errorResponseWriter;

    public AuthEntryPointJwt(ErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        errorResponseWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED, "Authentication is required to access this resource");
    }
}
