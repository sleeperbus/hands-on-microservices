package se.magnus.api.composite.product;

import org.springframework.web.bind.annotation.*;

public interface ProductCompositeService {
    @GetMapping(value = "/product-composite/{productId}", produces = "application/json")
    ProductAggregate getProduct(@PathVariable int productId);

    @PostMapping(value = "/product-composite", consumes = "application/json")
    void createCompositeProduct(@RequestBody ProductAggregate body);

    @DeleteMapping(value = "/product-composite/{productId}")
    void deleteCompositeProduct(@PathVariable int productId);
}
