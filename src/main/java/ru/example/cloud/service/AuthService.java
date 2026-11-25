package ru.example.cloud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.example.cloud.entity.AuthToken;
import ru.example.cloud.entity.User;
import ru.example.cloud.exception.AppException;
import ru.example.cloud.repository.AuthTokenRepository;
import ru.example.cloud.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthTokenRepository tokenRepository;

    @Transactional
    public String login(String login, String password) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new AppException(400, "Bad credentials"));

        if (!user.getPassword().equals(password)) {
            throw new AppException(400, "Bad credentials");
        }

        String token = UUID.randomUUID().toString();
        tokenRepository.save(new AuthToken(token, user, LocalDateTime.now()));
        return token;
    }

    @Transactional
    public void logout(String token) {
        tokenRepository.deleteById(token);
    }

    @Transactional(readOnly = true)
    public String getLoginByToken(String token) {
        AuthToken authToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException(401, "Unauthorized error"));
        return authToken.getUser().getLogin();
    }
}