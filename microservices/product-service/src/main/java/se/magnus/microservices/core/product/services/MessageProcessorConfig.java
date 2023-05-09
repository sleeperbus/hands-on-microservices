package se.magnus.microservices.core.product.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.event.Event;
import se.magnus.api.exceptions.EventProcessingException;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class MessageProcessorConfig {
    private final ProductService productService;

    public MessageProcessorConfig(ProductService productService) {
        this.productService = productService;
    }

    @Bean
    public Consumer<Event<Integer, Product>> messageProcessor() {
        return event -> {
            log.info("Process message created at {}...", event.getEventCreatedAt());
            switch (event.getEventType()) {
                case CREATE -> {
                    Product product = event.getData();
                    log.info("Create product with ID: {}", product.getProductId());
                    productService.createProduct(product).block();
                }
                case DELETE -> {
                    int productId = event.getKey();
                    log.info("Delete product with productId: {}", productId);
                    productService.deleteProduct(productId).block();
                }
                default -> {
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", exepcted a CREATE or DELETE event";
                    log.warn(errorMessage);
                    throw new EventProcessingException(errorMessage);
                }
            }
            log.info("Message processing done!");
        };
    }
}
