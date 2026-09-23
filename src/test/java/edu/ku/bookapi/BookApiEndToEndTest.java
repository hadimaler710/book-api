package edu.ku.bookapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests over real HTTP (random port, full context, in-memory H2
 * with 5 seeded books). Tests are ordered because they share database state:
 * reads first, then mutations, deletion last.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookApiEndToEndTest {

    private static final String BASE_URL = "/api/v1/books";
    private static final String CLEAN_CODE_ISBN = "9780132350884"; // seeded

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private ResponseEntity<String> get(String url) {
        return restTemplate.getForEntity(url, String.class);
    }

    private ResponseEntity<String> post(String json) {
        return restTemplate.postForEntity(BASE_URL, jsonEntity(json), String.class);
    }

    private ResponseEntity<String> put(long id, String json) {
        return restTemplate.exchange(BASE_URL + "/" + id, HttpMethod.PUT, jsonEntity(json), String.class);
    }

    private ResponseEntity<String> delete(long id) {
        return restTemplate.exchange(BASE_URL + "/" + id, HttpMethod.DELETE, HttpEntity.EMPTY, String.class);
    }

    private HttpEntity<String> jsonEntity(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(json, headers);
    }

    private JsonNode json(ResponseEntity<String> response) {
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse response body", e);
        }
    }

    private String validBookJson(String isbn) {
        return "{\"title\":\"Refactoring\",\"author\":\"Martin Fowler\",\"isbn\":\"" + isbn
                + "\",\"publishedYear\":2018,\"category\":\"Programming\"}";
    }

    @Test
    @Order(1)
    void getAllBooksReturnsSeededCatalogue() {
        ResponseEntity<String> response = get(BASE_URL);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode body = json(response);
        assertThat(body.get("content").size()).isEqualTo(5);
        assertThat(body.get("totalElements").asLong()).isEqualTo(5);
        assertThat(body.get("totalPages").asInt()).isEqualTo(1);
    }

    @Test
    @Order(2)
    void getAllBooksSupportsPagination() {
        ResponseEntity<String> firstPage = get(BASE_URL + "?page=0&size=2&sort=id,asc");

        assertThat(firstPage.getStatusCode().value()).isEqualTo(200);
        JsonNode first = json(firstPage);
        assertThat(first.get("content").size()).isEqualTo(2);
        assertThat(first.get("totalPages").asInt()).isEqualTo(3);
        assertThat(first.get("first").asBoolean()).isTrue();
        assertThat(first.get("hasNext").asBoolean()).isTrue();

        ResponseEntity<String> lastPage = get(BASE_URL + "?page=2&size=2&sort=id,asc");

        JsonNode last = json(lastPage);
        assertThat(last.get("content").size()).isEqualTo(1);
        assertThat(last.get("last").asBoolean()).isTrue();
        assertThat(last.get("hasPrevious").asBoolean()).isTrue();
    }

    @Test
    @Order(3)
    void getBookByIdReturns200() {
        ResponseEntity<String> response = get(BASE_URL + "/1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode body = json(response);
        assertThat(body.get("title").asText()).isEqualTo("Clean Code");
        assertThat(body.get("isbn").asText()).isEqualTo(CLEAN_CODE_ISBN);
        assertThat(body.get("createdAt").isNull()).isFalse();
        assertThat(body.get("updatedAt").isNull()).isFalse();
    }

    @Test
    @Order(4)
    void getBookByIdReturns404WithErrorBody() {
        ResponseEntity<String> response = get(BASE_URL + "/999");

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        JsonNode body = json(response);
        assertThat(body.get("status").asInt()).isEqualTo(404);
        assertThat(body.get("error").asText()).isEqualTo("Not Found");
        assertThat(body.get("message").asText()).contains("999");
        assertThat(body.get("path").asText()).isEqualTo(BASE_URL + "/999");
    }

    @Test
    @Order(5)
    void createBookReturns201AndPersists() {
        ResponseEntity<String> response = post(validBookJson("9780321125217"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getHeaders().getFirst("Location")).contains("/api/v1/books/");
        JsonNode created = json(response);
        assertThat(created.get("id").asLong()).isEqualTo(6);

        assertThat(get(BASE_URL + "/6").getStatusCode().value()).isEqualTo(200);
        assertThat(json(get(BASE_URL)).get("totalElements").asLong()).isEqualTo(6);
    }

    @Test
    @Order(6)
    void createBookRejectsDuplicateIsbn() {
        ResponseEntity<String> response = post(validBookJson(CLEAN_CODE_ISBN));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(json(response).get("message").asText()).contains(CLEAN_CODE_ISBN);
    }

    @Test
    @Order(7)
    void createBookReturns400WithFieldErrors() {
        String invalid = "{\"title\":\"\",\"author\":\"" + "A".repeat(150)
                + "\",\"isbn\":\"123\",\"publishedYear\":1800,\"category\":\"\"}";

        ResponseEntity<String> response = post(invalid);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        JsonNode body = json(response);
        assertThat(body.get("validationErrors").get("title").asText()).isNotEmpty();
        assertThat(body.get("validationErrors").get("author").asText()).isNotEmpty();
        assertThat(body.get("validationErrors").get("isbn").asText()).isNotEmpty();
        assertThat(body.get("validationErrors").get("publishedYear").asText()).isNotEmpty();
        assertThat(body.get("validationErrors").get("category").asText()).isNotEmpty();
    }

    @Test
    @Order(8)
    void updateBookReturns200AndRefreshesCachedReads() {
        // Warm the single-book cache, then update, then read again
        assertThat(json(get(BASE_URL + "/2")).get("title").asText()).isEqualTo("Effective Java");

        String update = "{\"title\":\"Effective Java (3rd Edition)\",\"author\":\"Joshua Bloch\","
                + "\"isbn\":\"9780134685991\",\"publishedYear\":2018,\"category\":\"Programming\"}";
        ResponseEntity<String> response = put(2, update);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).get("title").asText()).isEqualTo("Effective Java (3rd Edition)");
        assertThat(json(response).get("updatedAt").asText()).isNotBlank();

        // Proves @CachePut: the cached entry must not serve the stale title
        assertThat(json(get(BASE_URL + "/2")).get("title").asText())
                .isEqualTo("Effective Java (3rd Edition)");
    }

    @Test
    @Order(9)
    void updateBookReturns404WhenMissing() {
        ResponseEntity<String> response = put(999, validBookJson("9780321125217"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(json(response).get("message").asText()).contains("999");
    }

    @Test
    @Order(10)
    void updateBookReturns400ForInvalidData() {
        String invalid = "{\"title\":\"X\",\"author\":\"Y\",\"isbn\":\"9780134685991\","
                + "\"publishedYear\":1800,\"category\":\"Z\"}";

        ResponseEntity<String> response = put(2, invalid);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(json(response).get("validationErrors").get("publishedYear").asText()).isNotEmpty();
    }

    @Test
    @Order(11)
    void deleteBookReturns204Then404() {
        ResponseEntity<String> response = delete(5);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(get(BASE_URL + "/5").getStatusCode().value()).isEqualTo(404);
        assertThat(delete(5).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @Order(12)
    void unknownPathReturns404() {
        ResponseEntity<String> response = get("/api/v1/unknown");

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(json(response).get("path").asText()).isEqualTo("/api/v1/unknown");
    }

    @Test
    @Order(13)
    void invalidSortPropertyReturns400() {
        ResponseEntity<String> response = get(BASE_URL + "?sort=banana,asc");

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(json(response).get("message").asText()).contains("banana");
    }

    @Test
    @Order(14)
    void swaggerUiAndOpenApiDocsAreAvailable() {
        ResponseEntity<String> docs = get("/v3/api-docs");

        assertThat(docs.getStatusCode().value()).isEqualTo(200);
        assertThat(docs.getBody()).contains("openapi");

        ResponseEntity<String> swagger = get("/swagger-ui/index.html");
        assertThat(swagger.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @Order(15)
    void actuatorHealthIsUp() {
        ResponseEntity<String> response = get("/actuator/health");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).get("status").asText()).isEqualTo("UP");
    }
}