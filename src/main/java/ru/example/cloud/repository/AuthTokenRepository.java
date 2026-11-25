package ru.example.cloud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.example.cloud.entity.AuthToken;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {
    Optional<AuthToken> findByToken(String token);
}