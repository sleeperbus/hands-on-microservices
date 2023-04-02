package se.magnus.microservices.core.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.review.Review;
import se.magnus.microservices.core.review.persistent.ReviewRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class ReviewApplicationTests {
    @Autowired
    private WebTestClient client;

    @Autowired
    private ReviewRepository repository;

    @BeforeEach
    public void setupDb() {
        repository.deleteAll();
    }

    @Test
    public void getReviewsByProductId() {
        int productId = 1;

        assertEquals(0, repository.count());

        postAndVerifyReview(productId, 1, OK);
        postAndVerifyReview(productId, 2, OK);
        postAndVerifyReview(productId, 3, OK);

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

        postAndVerifyReview(productId, reviewId, OK)
                .jsonPath("$.productId").isEqualTo(productId)
                .jsonPath("$.reviewId").isEqualTo(reviewId);

        assertEquals(1, repository.count());

        postAndVerifyReview(productId, reviewId, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/review")
                .jsonPath("$.message").isEqualTo("Duplicate key, product Id: 1, Review Id: 1");

        assertEquals(1, repository.count());
    }
    
    @Test
    public void deleteReview() {
        int productId = 1;
        int reviewId = 1;

        postAndVerifyReview(productId, reviewId, OK);
        assertEquals(1, repository.findByProductId(productId).size());
        
        deleteAndVerifyReviewsByProductId(productId, OK);
        assertEquals(0, repository.findByProductId(productId).size());

        deleteAndVerifyReviewsByProductId(productId, OK);
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

    private WebTestClient.BodyContentSpec postAndVerifyReview(int productId, int reviewId, HttpStatus httpStatus) {
        Review review = new Review(productId, reviewId, "Author " + productId, "S", "C", "");
        return client.post()
                .uri("/review")
                .body(just(review), Review.class)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(httpStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }

    private WebTestClient.BodyContentSpec deleteAndVerifyReviewsByProductId(int productId, HttpStatus httpStatus) {
        return client.delete()
                .uri("/review?productId=" + productId)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(httpStatus)
                .expectBody();
    }
}
