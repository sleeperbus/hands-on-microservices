package se.magnus.api.core.recommendation;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

public interface RecommendationService {
    @GetMapping(value = "/recommendation", produces = "application/json")
    Flux<Recommendation> getRecommendation(@RequestParam(value = "productId", required = true) int productId);

    @PostMapping(value = "/recommendation", consumes = "application/json", produces = "application/json")
    Recommendation createRecommendation(@RequestBody Recommendation body);

    @DeleteMapping(value = "recommendation")
    void deleteRecommendation(@RequestParam(value = "productId", required = true) int productId);
}
