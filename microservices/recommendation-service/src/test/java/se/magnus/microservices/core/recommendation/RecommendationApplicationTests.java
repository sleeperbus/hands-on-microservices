package se.magnus.microservices.core.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.event.Event;
import se.magnus.microservices.core.recommendation.persistent.RecommendationRepository;
import se.magnus.util.exceptions.InvalidInputException;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecommendationApplicationTests {
    @Autowired
    private WebTestClient client;

    @Autowired
    private RecommendationRepository repository;

    @Autowired
    @Qualifier("messageProcessor")
    private Consumer<Event<Integer, Recommendation>> messageProcessor;

    @BeforeEach
    public void setupDb() {
        repository.deleteAll().block();
    }

    @Test
    public void getRecommendationsByProductId() {
        int productId = 1;

        sendCreateRecommendationEvent(productId, 1);
        sendCreateRecommendationEvent(productId, 2);
        sendCreateRecommendationEvent(productId, 3);

        assertEquals(3, repository.findByProductId(productId).count().block());

        getAndVerifyRecommendationsByProductId(productId, OK)
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[0].productId").isEqualTo(productId);
    }


    @Test
    public void getRecommendationsMissingParameter() {
        getAndVerifyRecommendationsByProductId("", BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/recommendation")
                .jsonPath("$.message").isEqualTo("Required query parameter 'productId' is not present.");
    }



    @Test
    public void getRecommendationsInvalidParameter() {
        getAndVerifyRecommendationsByProductId("?productId=no-integer", BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/recommendation")
                .jsonPath("$.message").isEqualTo("Type mismatch.");
    }

    @Test
    public void getRecommendationsNotFound() {
        int productIdNotFound = 113;
        getAndVerifyRecommendationsByProductId(productIdNotFound, OK)
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    public void getRecommendationInvalidParameterNegativeValue() {
        int productNegativeId = -1;

        getAndVerifyRecommendationsByProductId(productNegativeId, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/recommendation")
                .jsonPath("$.message").isEqualTo("Invalid productId: " + productNegativeId);
    }

    @Test
    public void duplicateError() {
        int productId = 1;
        int recommendationId = 1;

        sendCreateRecommendationEvent(productId, recommendationId);

        assertEquals(1, repository.count().block());

        InvalidInputException thrown = assertThrows(InvalidInputException.class, () -> sendCreateRecommendationEvent(productId, recommendationId), "Expected a InvalidInputException here!");
        assertEquals("Duplicate key, Product Id: 1, Recommendation Id: 1", thrown.getMessage());
    }

    @Test
    public void deleteRecommendations() {
        int productId = 1;
        int recommendationId = 1;

        sendCreateRecommendationEvent(productId, recommendationId);
        assertEquals(1, repository.count().block());

        sendDeleteRecommendationEvent(productId);
        assertEquals(0, repository.count().block());

        sendDeleteRecommendationEvent(productId);
    }

    private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(int productId, HttpStatus httpStatus) {
        return getAndVerifyRecommendationsByProductId("?productId=" + productId, httpStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(String productIdQuery, HttpStatus httpStatus) {
        return client.get()
                .uri("/recommendation" + productIdQuery)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(httpStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody()
                ;
    }

    private void sendCreateRecommendationEvent(int productId, int recommendationId) {
        Recommendation recommendation = new Recommendation(productId, recommendationId, "Author " + recommendationId, recommendationId, "C", "SA");
        Event<Integer, Recommendation> event = new Event(Event.Type.CREATE, productId, recommendation);
        messageProcessor.accept(event);
    }

    private void sendDeleteRecommendationEvent(int productId) {
        Event<Integer, Recommendation> event = new Event(Event.Type.DELETE, productId, null);
        messageProcessor.accept(event);
    }
}
