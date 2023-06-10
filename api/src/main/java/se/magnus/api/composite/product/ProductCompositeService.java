package se.magnus.api.composite.product;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

public interface ProductCompositeService {
    @GetMapping(value = "/product-composite/{productId}", produces = "application/json")
    Mono<ProductAggregate> getCompositeProduct(@PathVariable int productId);

    @PostMapping(value = "/product-composite", consumes = "application/json")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> createCompositeProduct(@RequestBody ProductAggregate body);

    @DeleteMapping(value = "/product-composite/{productId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> deleteCompositeProduct(@PathVariable int productId);
}
