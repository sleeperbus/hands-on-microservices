package se.magnus.microservices.core.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.review.Review;
import se.magnus.api.event.Event;
import se.magnus.microservices.core.review.persistent.ReviewRepository;
import se.magnus.util.exceptions.InvalidInputException;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {"eureka.client.enabled=false"})
class ReviewApplicationTests {
    @Autowired
    private WebTestClient client;

    @Autowired
    private ReviewRepository repository;

    @Autowired
    @Qualifier("messageProcessor")
    private Consumer<Event<Integer, Review>> messageProcessor;

    @BeforeEach
    public void setupDb() {
        repository.deleteAll();
    }

    @Test
    public void getReviewsByProductId() {
        int productId = 1;

        assertEquals(0, repository.count());

        sendCreateReviewEvent(productId, 1);
        sendCreateReviewEvent(productId, 2);
        sendCreateReviewEvent(productId, 3);

        assertEquals(3, repository.count());

        getAndVerifyReviewsByProductId(productId, OK)
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[2].productId").isEqualTo(productId)
                .jsonPath("$[2].reviewId").isEqualTo(3);
    }


    @Test
    public void getReviewsMissingParameter() {
        getAndVerifyReviewsByProductId("", BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/review")
                .jsonPath("$.message").isEqualTo("Required query parameter 'productId' is not present.");
    }

    @Test
    public void getReviewsInvalidParameter() {
        getAndVerifyReviewsByProductId("?productId=no-integer", BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/review")
                .jsonPath("$.message").isEqualTo("Type mismatch.");
    }

    @Test
    public void getReviewsNotFound() {
        int productIdNotFound = 213;
        getAndVerifyReviewsByProductId(productIdNotFound, OK)
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    public void getReviewsInvalidParameterNegativeValue() {

        int productIdInvalid = -1;

        client.get()
                .uri("/review?productId=" + productIdInvalid)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(UNPROCESSABLE_ENTITY)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.path").isEqualTo("/review")
                .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
    }

    @Test
    public void duplicateError() {
        int productId = 1;
        int reviewId = 1;

        assertEquals(0, repository.count());

        sendCreateReviewEvent(1, 1);
        assertEquals(1, repository.count());

        InvalidInputException thrown = assertThrows(InvalidInputException.class, () -> sendCreateReviewEvent(productId, reviewId), "Expected a InvalidInputException here!");
        assertEquals("Duplicate key, product Id: 1, Review Id: 1", thrown.getMessage());
        assertEquals(1, repository.count());
    }
    
    @Test
    public void deleteReview() {
        int productId = 1;
        int reviewId = 1;

        sendCreateReviewEvent(productId, reviewId);
        assertEquals(1, repository.findByProductId(productId).size());
        
        sendDeleteReviewEvent(productId);
        assertEquals(0, repository.findByProductId(productId).size());

        sendDeleteReviewEvent(productId);
    }

    private void sendCreateReviewEvent(int productId, int reviewId) {
        Review review = new Review(productId, reviewId, "Author " + productId, "S", "C", "SA");
        Event<Integer, Review> event = new Event(Event.Type.CREATE, productId, review);
        messageProcessor.accept(event);
    }

    private void sendDeleteReviewEvent(int productId) {
        Event<Integer, Review> event = new Event(Event.Type.DELETE, productId, null);
        messageProcessor.accept(event);
    }


    private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(int productId, HttpStatus httpStatus) {
        return getAndVerifyReviewsByProductId("?productId=" + productId, httpStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(String productQuery, HttpStatus httpStatus) {
        return client.get()
                .uri("/review" + productQuery)
                .exchange()
                .expectStatus().isEqualTo(httpStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }
}
