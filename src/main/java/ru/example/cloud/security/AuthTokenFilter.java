package ru.example.cloud.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.example.cloud.exception.AppException;
import ru.example.cloud.service.AuthService;

@Component
@RequiredArgsConstructor
public class AuthTokenFilter implements HandlerInterceptor {

    private AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("auth-token");
        if (token == null || token.isBlank()) {
            return unauthorized(response);
        }

        try {
            String login = authService.getLoginByToken(token);
            request.setAttribute("login", login);
            return true;
        } catch (AppException e) {
            return unauthorized(response);
        }
    }

    private boolean unauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\": \"Unauthorized error\", \"id\": 0}");
        return false;
    }
}