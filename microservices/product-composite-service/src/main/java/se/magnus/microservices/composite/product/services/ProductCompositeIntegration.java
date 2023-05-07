package se.magnus.microservices.composite.product.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatusCode;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.api.event.Event;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.exceptions.NotFoundException;
import se.magnus.util.http.HttpErrorInfo;

import java.io.IOException;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static reactor.core.publisher.Mono.empty;
import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.api.event.Event.Type.DELETE;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;
    private final WebClient webClient;
    private final StreamBridge streamBridge;
    private final Scheduler publishEventScheduler;

    @Autowired
    public ProductCompositeIntegration(
            RestTemplate restTemplate,
            ObjectMapper mapper,
            @Qualifier("publishEventScheduler") Scheduler publishEventScheduler,
            @Value("${app.product-service.host}") String productServiceUrl,
            @Value("${app.product-service.port}") String productServicePort,
            @Value("${app.recommendation-service.host}") String recommendationServiceUrl,
            @Value("${app.recommendation-service.port}") String recommendationServicePort,
            @Value("${app.review-service.host}") String reviewServiceUrl,
            @Value("${app.review-service.port}") String reviewServicePort,

            WebClient.Builder webClient, StreamBridge streamBridge) {
        this.publishEventScheduler = publishEventScheduler;
        this.restTemplate = restTemplate;
        this.mapper = mapper;
        this.webClient = webClient.build();
        this.streamBridge = streamBridge;
        this.productServiceUrl = "http://" + productServiceUrl + ":" + productServicePort + "/product";
        this.recommendationServiceUrl = "http://" + recommendationServiceUrl + ":" + recommendationServicePort + "/recommendation";
        this.reviewServiceUrl = "http://" + reviewServiceUrl + ":" + reviewServicePort + "/review";
    }

    @Override
    public Mono<Product> getProduct(int productId) {
        String url = productServiceUrl + "/" + productId;
        LOG.debug("Will call getProduct API on URL: {}", url);

        return webClient.get().uri(url)
                .retrieve()
                .bodyToMono(Product.class)
                .log()
                .onErrorMap(WebClientResponseException.class, this::handleException);
    }

    private Throwable handleException(Throwable ex) {

        if (!(ex instanceof WebClientResponseException wcre)) {
            LOG.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
            return ex;
        }

        HttpStatusCode statusCode = wcre.getStatusCode();
        if (statusCode.equals(NOT_FOUND)) {
            return new NotFoundException(getErrorMessage(wcre));
        } else if (statusCode.equals(UNPROCESSABLE_ENTITY)) {
            return new InvalidInputException(getErrorMessage(wcre));
        }
        LOG.warn("Got a unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
        LOG.warn("Error body: {}", wcre.getResponseBodyAsString());
        return ex;
    }

    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }

    @Override
    public Product createProduct(Product body) {
        return Mono.fromCallable(() -> {
            sendMessage("products-out-0", new Event(CREATE, body.getProductId(), body));
            return body;
        }).subscribeOn(publishEventScheduler).share().block();
    }


    @Override
    public void deleteProduct(int productId) {
        Mono.fromRunnable(() -> {
            sendMessage("products-out-0", new Event(DELETE, productId, null));
        }).subscribeOn(publishEventScheduler).share().block();
    }


    private String getErrorMessage(HttpClientErrorException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }

    @Override
    public Flux<Recommendation> getRecommendation(int productId) {
        String url = recommendationServiceUrl + "?productId=" + productId;
        LOG.debug("will call getRecommendation API on URL: {}", url);

        return webClient.get().uri(url)
                .retrieve()
                .bodyToFlux(Recommendation.class)
                .log()
                .onErrorResume(error -> empty());
    }

    @Override
    public Recommendation createRecommendation(Recommendation body) {
        return Mono.fromCallable(() -> {
            sendMessage("recommendations-out-0", new Event(CREATE, body.getProductId(), body));
            return body;
        }).subscribeOn(publishEventScheduler).share().block();
    }

    @Override
    public void deleteRecommendation(int productId) {
        Mono.fromRunnable(() -> sendMessage("recommendations-out-0", new Event(DELETE, productId, null)))
                .subscribeOn(publishEventScheduler).share().block();
    }

    @Override
    public Flux<Review> getReviews(int productId) {
        String url = reviewServiceUrl + "?productId=" + productId;
        LOG.debug("will call getReviews API on URL: {}", url);

        return webClient.get().uri(url)
                .retrieve()
                .bodyToFlux(Review.class)
                .log()
                .onErrorResume(error -> empty());
    }

    @Override
    public Review createReview(Review body) {
        return Mono.fromCallable(() -> {
            sendMessage("reviews-out-0", new Event(CREATE, body.getProductId(), body));
            return body;
        }).subscribeOn(publishEventScheduler).share().block();
    }

    @Override
    public void deleteReviews(int productId) {
        Mono.fromRunnable(() -> sendMessage("reviews-out-0", new Event(DELETE, productId, null)))
                .subscribeOn(publishEventScheduler).share().block();
    }

    private RuntimeException handleHttpClientException(HttpClientErrorException e) {
        HttpStatusCode statusCode = e.getStatusCode();
        if (NOT_FOUND.equals(statusCode)) {
            return new NotFoundException(getErrorMessage(e));
        } else if (UNPROCESSABLE_ENTITY.equals(statusCode)) {
            return new InvalidInputException(getErrorMessage(e));
        }
        LOG.warn("Go a unexpected HTTP error: {}, will rethrow it", e.getStatusCode());
        LOG.warn("Error body: {}", e.getResponseBodyAsString());
        return e;
    }

    private void sendMessage(String bindingName, Event event) {
        LOG.debug("Sending a {} message to {}", event.getEventType(), bindingName);
        Message message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.getKey())
                .build();
        streamBridge.send(bindingName, message);
    }
}
