Thesis-Cloud-Storage# (Дипломный проект)
REST-сервис «Облачное хранилище»: загрузка, скачивание, переименование, удаление файлов и вывод списка файлов пользователя. Реализована авторизация по токену (заголовок auth-token) в соответствии со спецификацией OpenAPI Cloud API.

Стек
Java 17, Spring Boot 3.3 (Web, Data JPA, Validation)
PostgreSQL 16
Maven
Docker / docker-compose
Тесты: JUnit 5, Mockito, Testcontainers
Запуск
Требуется Docker и Docker Compose.

После запуска бэкенд доступен по адресу: http://localhost:8080/cloud Фронт-приложение запускается отдельно на http://localhost:8081.

Тестовый пользователь: логин user, пароль 12345.user

Проверка через curl
# Логин
curl -X POST http://localhost:8080/cloud/login \
  -H "Content-Type: application/json" \
  -d '{"login":"user","password":"12345"}'
# -> {"auth-token":"<uuid>"}

# Список файлов
curl "http://localhost:8080/cloud/list?limit=10" -H "auth-token: <токен>"

# Загрузка файла
curl -X POST "http://localhost:8080/cloud/file?filename=test.txt" \
  -H "auth-token: <токен>" -F "file=@test.txt"

# Скачивание
curl -o out.txt "http://localhost:8080/cloud/file?filename=test.txt" \
  -H "auth-token: <токен>"

# Переименование
curl -X PUT "http://localhost:8080/cloud/file?filename=test.txt" \
  -H "auth-token: <токен>" -H "Content-Type: application/json" \
  -d '{"name":"renamed.txt"}'

# Удаление
curl -X DELETE "http://localhost:8080/cloud/file?filename=renamed.txt" \
  -H "auth-token: <токен>"

# Выход
curl -X POST http://localhost:8080/cloud/logout -H "auth-token: <токен>"
Тесты
mvn test
Юнит-тесты (Mockito) охватывают AuthService и FileStorageService. Интеграционные тесты (Testcontainers) проверяют полный сценарий: вход → загрузка → список → удаление.

Структура проекта
src/main/java/ru/example/cloud/
├── CloudApplication.java
├── config/      (CORS, хранилище, инициализация данных)
├── controller/  (AuthController, FileController)
├── dto/         (LoginRequest, LoginResponse, ErrorDto, ...)
├── entity/      (User, StorageFile, AuthToken)
├── repository/  (JPA-репозитории)
├── security/    (AuthTokenFilter)
├── service/     (AuthService, FileStorageService)
└── exception/   (AppException, GlobalExceptionHandler)
