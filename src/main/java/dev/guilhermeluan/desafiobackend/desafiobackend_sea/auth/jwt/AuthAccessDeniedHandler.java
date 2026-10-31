package dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.jwt;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.exception.ErrorCode;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.exception.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthAccessDeniedHandler implements AccessDeniedHandler {

    private final ErrorResponseWriter errorResponseWriter;

    public AuthAccessDeniedHandler(ErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        errorResponseWriter.write(request, response, HttpServletResponse.SC_FORBIDDEN,
                ErrorCode.FORBIDDEN, "You do not have permission to access this resource");
    }
}
