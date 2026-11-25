package ru.example.cloud.integration;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CloudIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate rest;

    private static String token;

    @Test
    @Order(1)
    void loginReturnsAuthToken() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/cloud/login",
                Map.of("login", "user", "password", "12345"),
                Map.class);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("auth-token"));

        token = (String) response.getBody().get("auth-token");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @Order(2)
    void uploadAndListFile() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("auth-token", token);

        ByteArrayResource fileResource = new ByteArrayResource("integration test content".getBytes()) {
            @Override
            public String getFilename() {
                return "test.txt";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpHeaders multipartHeaders = new HttpHeaders();
        multipartHeaders.set("auth-token", token);
        multipartHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<Void> uploadResponse = rest.exchange(
                "/cloud/file?filename=test.txt",
                HttpMethod.POST,
                new HttpEntity<>(body, multipartHeaders),
                Void.class);

        assertEquals(200, uploadResponse.getStatusCode().value());

        ResponseEntity<List> listResponse = rest.exchange(
                "/cloud/list?limit=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class);

        assertEquals(200, listResponse.getStatusCode().value());
        List<?> files = listResponse.getBody();
        assertNotNull(files);
        assertFalse(files.isEmpty());

        @SuppressWarnings("unchecked")
        Map<String, Object> firstFile = (Map<String, Object>) files.get(0);
        assertEquals("test.txt", firstFile.get("filename"));
        assertEquals(23, ((Number) firstFile.get("size")).intValue());
    }

    @Test
    @Order(3)
    void downloadReturnsFileContent() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("auth-token", token);

        ResponseEntity<byte[]> response = rest.exchange(
                "/cloud/file?filename=test.txt",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("integration test content", new String(response.getBody()));
    }

    @Test
    @Order(4)
    void renameFile() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("auth-token", token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Void> response = rest.exchange(
                "/cloud/file?filename=test.txt",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("name", "renamed.txt"), headers),
                Void.class);

        assertEquals(200, response.getStatusCode().value());

        ResponseEntity<List> listResponse = rest.exchange(
                "/cloud/list?limit=10",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                List.class);
        HttpHeaders listHeaders = new HttpHeaders();
        listHeaders.set("auth-token", token);
        listResponse = rest.exchange(
                "/cloud/list?limit=10",
                HttpMethod.GET,
                new HttpEntity<>(listHeaders),
                List.class);

        List<?> files = listResponse.getBody();
        assertNotNull(files);
        boolean found = files.stream()
                .anyMatch(f -> "renamed.txt".equals(((Map<?, ?>) f).get("filename")));
        assertTrue(found);
    }

    @Test
    @Order(5)
    void deleteFile() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("auth-token", token);

        ResponseEntity<Void> response = rest.exchange(
                "/cloud/file?filename=renamed.txt",
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class);

        assertEquals(200, response.getStatusCode().value());

        ResponseEntity<List> listResponse = rest.exchange(
                "/cloud/list?limit=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class);
        List<?> files = listResponse.getBody();
        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    @Order(6)
    void requestWithUnknownTokenReturns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("auth-token", "definitely-invalid-token");

        ResponseEntity<String> response = rest.exchange(
                "/cloud/list?limit=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    @Order(7)
    void logoutInvalidatesToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("auth-token", token);

        ResponseEntity<Void> logoutResponse = rest.exchange(
                "/cloud/logout",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Void.class);
        assertEquals(200, logoutResponse.getStatusCode().value());

        ResponseEntity<String> afterLogout = rest.exchange(
                "/cloud/list?limit=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        assertEquals(401, afterLogout.getStatusCode().value());
    }

    @Test
    @Order(8)
    void loginWithBadCredentialsReturns400() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/cloud/login",
                Map.of("login", "user", "password", "wrong"),
                Map.class);

        assertEquals(400, response.getStatusCode().value());
    }
}