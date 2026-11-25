package ru.example.cloud.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.example.cloud.entity.AuthToken;
import ru.example.cloud.entity.User;
import ru.example.cloud.exception.AppException;
import ru.example.cloud.repository.AuthTokenRepository;
import ru.example.cloud.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthTokenRepository tokenRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
    }

    @Test
    void loginWithCorrectCredentialsReturnsToken() {
        when(userRepository.findByLogin("user")).thenReturn(Optional.of(testUser));

        String token = authService.login("user", "12345");

        assertNotNull(token);
        assertTrue(!token.isBlank());
        verify(tokenRepository).save(any(AuthToken.class));
    }

    @Test
    void loginWithUnknownLoginThrows400() {
        when(userRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> authService.login("unknown", "12345"));

        assertEquals(400, ex.getStatus());
        assertEquals("Bad credentials", ex.getMessage());
        verify(tokenRepository, never()).save(any(AuthToken.class));
    }

    @Test
    void loginWithWrongPasswordThrows400() {
        when(userRepository.findByLogin("user")).thenReturn(Optional.of(testUser));

        AppException ex = assertThrows(AppException.class,
                () -> authService.login("user", "wrong"));

        assertEquals(400, ex.getStatus());
        assertEquals("Bad credentials", ex.getMessage());
        verify(tokenRepository, never()).save(any(AuthToken.class));
    }

    @Test
    void logoutDeletesToken() {
        String token = "some-token";

        authService.logout(token);

        verify(tokenRepository).deleteById(token);
    }

    @Test
    void getLoginByTokenReturnsUserLogin() {
        AuthToken authToken = new AuthToken();
        when(tokenRepository.findByToken("some-token")).thenReturn(Optional.of(authToken));

        String login = authService.getLoginByToken("some-token");

        assertEquals("user", login);
    }

    @Test
    void getLoginByUnknownTokenThrows401() {
        when(tokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> authService.getLoginByToken("bad-token"));

        assertEquals(401, ex.getStatus());
        assertEquals("Unauthorized error", ex.getMessage());
    }
}