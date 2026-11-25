package ru.example.cloud.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.example.cloud.entity.User;
import ru.example.cloud.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.findByLogin("user").isEmpty()) {
            userRepository.save(new User(null, "user", "12345"));
        }
    }
}