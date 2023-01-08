package se.magnus.microservices.core.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ComponentScan("se.magnus")
class ProductServiceApplicationTests {
    @Autowired
    private WebTestClient client;

    @Test
    public void getProductById() {
        int productId = 1;

        client.get()
                .uri("/product/" + productId)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.productId").isEqualTo(productId);
    }

    @Test
    public void getProductNotFound() {
        int productIdNotFound = 13;

        client.get()
                .uri("/product/" + productIdNotFound)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.path").isEqualTo("/product/" + productIdNotFound)
                .jsonPath("$.message").isEqualTo("No product found for productId: " + productIdNotFound);
    }

    @Test
    public void getProductInvalidParameterNegativeValue() {
        int productInvalidId = -1;

        client.get()
                .uri("/product/" + productInvalidId)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.path").isEqualTo("/product/" + productInvalidId)
                .jsonPath("$.message").isEqualTo("Invalid productId: " + productInvalidId);
    }
}
