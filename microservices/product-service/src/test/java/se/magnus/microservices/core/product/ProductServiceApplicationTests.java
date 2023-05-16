package se.magnus.microservices.core.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;
import se.magnus.api.core.product.Product;
import se.magnus.api.event.Event;
import se.magnus.microservices.core.product.persistent.ProductRepository;
import se.magnus.util.exceptions.InvalidInputException;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import static reactor.core.publisher.Mono.just;
import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.api.event.Event.Type.DELETE;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ComponentScan("se.magnus")
class ProductServiceApplicationTests {
    @Autowired
    private WebTestClient client;

    @Autowired
    private ProductRepository repository;

    @Autowired
    @Qualifier("messageProcessor")
    private Consumer<Event<Integer, Product>> messageProcessor;

    @BeforeEach
    public void setupDb() {
        repository.deleteAll().block();
    }


    @Test
    public void getProductById() {
        int productId = 1;

        assertNull(repository.findByProductId(productId).block());
        assertEquals(0, repository.count().block());

        sendCreateProductEvent(productId);

        assertNotNull(repository.findByProductId(productId).block());
        assertEquals(1, repository.count().block());

        getAndVerifyProduct(productId, OK)
                .jsonPath("$.productId").isEqualTo(productId);
    }

    @Test
    public void getProductNotFound() {
        int productIdNotFound = 13;

        getAndVerifyProduct(13, NOT_FOUND)
                .jsonPath("$.path").isEqualTo("/product/" + productIdNotFound)
                .jsonPath("$.message").isEqualTo("No product found for productId: " + productIdNotFound);
    }

    @Test
    public void getProductInvalidParameterNegativeValue() {
        int productInvalidId = -1;

        getAndVerifyProduct(productInvalidId, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/product/" + productInvalidId)
                .jsonPath("$.message").isEqualTo("Invalid productId: " + productInvalidId);
    }

    @Test
    public void duplicateError() {
        int productId = 1;

        assertNull(repository.findByProductId(productId).block());

        sendCreateProductEvent(productId);
        assertNotNull(repository.findByProductId(productId).block());

        InvalidInputException thrown = assertThrows(InvalidInputException.class, () -> sendCreateProductEvent(productId), "Expected a InvalidInputException here!");
        assertEquals("Duplicate key, Product Id: " + productId, thrown.getMessage());
    }

    @Test
    public void deleteProduct() {
        int productId = 1;

        sendCreateProductEvent(productId);
        assertNotNull(repository.findByProductId(productId).block());

        sendDeleteProductEvent(productId);
        assertNull(repository.findByProductId(productId).block());

        // 멱등성
        sendDeleteProductEvent(productId);
    }


    private BodyContentSpec getAndVerifyProduct(int productId, HttpStatus httpStatus) {
        return client.get()
                .uri("/product/" + productId)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(httpStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }


    private void sendCreateProductEvent(int productId) {
        Product product = new Product(productId, "Name " + productId, productId, "SA");
        Event<Integer, Product> event = new Event(CREATE, productId, product);
        messageProcessor.accept(event);
    }

    private void sendDeleteProductEvent(int productId) {
        Event<Integer, Product> event = new Event(DELETE, productId, null);
        messageProcessor.accept(event);
    }
}
