package ru.example.cloud.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import ru.example.cloud.dto.LoginRequest;
import ru.example.cloud.dto.LoginResponse;
import ru.example.cloud.service.AuthService;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        String token = authService.login(request.getLogin(), request.getPassword());
        return new LoginResponse(token);
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader("auth-token") String token) {
        authService.logout(token);
    }
}