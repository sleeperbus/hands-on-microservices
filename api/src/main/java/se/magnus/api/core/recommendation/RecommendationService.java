package se.magnus.api.core.recommendation;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RecommendationService {
    @GetMapping(value = "/recommendation", produces = "application/json")
    Flux<Recommendation> getRecommendation(@RequestParam(value = "productId", required = true) int productId);

    @PostMapping(value = "/recommendation", consumes = "application/json", produces = "application/json")
    Mono<Recommendation> createRecommendation(@RequestBody Recommendation body);

    @DeleteMapping(value = "recommendation")
    Mono<Void> deleteRecommendation(@RequestParam(value = "productId", required = true) int productId);
}
