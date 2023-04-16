package se.magnus.microservices.composite.product.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.exceptions.NotFoundException;
import se.magnus.util.http.HttpErrorInfo;

import java.io.IOException;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static reactor.core.publisher.Mono.empty;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;

    private final WebClient webClient;

    @Autowired
    public ProductCompositeIntegration(
            RestTemplate restTemplate,
            ObjectMapper mapper,
            @Value("${app.product-service.host}") String productServiceUrl,
            @Value("${app.product-service.port}") String productServicePort,
            @Value("${app.recommendation-service.host}") String recommendationServiceUrl,
            @Value("${app.recommendation-service.port}") String recommendationServicePort,
            @Value("${app.review-service.host}") String reviewServiceUrl,
            @Value("${app.review-service.port}") String reviewServicePort,

            WebClient.Builder webClient) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
        this.webClient = webClient.build();
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
        try {
            String url = productServiceUrl;
            LOG.debug("Will post a new product to URL: {}", url);

            Product product = restTemplate.postForObject(url, body, Product.class);
            LOG.debug("Created a product with Id: {}", product.getProductId());
            return product;
        } catch (HttpClientErrorException e) {
            throw handleHttpClientException(e);
        }
    }


    @Override
    public void deleteProduct(int productId) {
        try {
            String url = productServiceUrl + "/" + productId;
            LOG.debug("Will call the deleteProduct API on URL: {}", url);
            restTemplate.delete(url);

        } catch (HttpClientErrorException e) {
            throw handleHttpClientException(e);
        }
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
        try {
            String url = recommendationServiceUrl;
            LOG.debug("Will post a new recommendation to URL: {}", url);

            Recommendation recommendation = restTemplate.postForObject(url, body, Recommendation.class);
            LOG.debug("Created a recommendation with product Id: {], recommendation Id: {}", recommendation.getProductId(), recommendation.getRecommendationId());
            return recommendation;
        } catch (HttpClientErrorException e) {
            throw handleHttpClientException(e);
        }
    }

    @Override
    public void deleteRecommendation(int productId) {
        try {
            String url = recommendationServiceUrl + "?productId=" + productId;
            LOG.debug("Will call the deleteRecommendation API on URL: {}", url);
            restTemplate.delete(url);
        } catch (HttpClientErrorException e) {
            throw handleHttpClientException(e);
        }
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
        try {
            String url = reviewServiceUrl;
            LOG.debug("Will post a new review to URL: {}", url);

            Review review = restTemplate.postForObject(url, body, Review.class);
            LOG.debug("Created a review with product Id: {}, review Id: {}", review.getProductId(), review.getReviewId());
            return review;
        } catch (HttpClientErrorException e) {
            throw handleHttpClientException(e);
        }
    }

    @Override
    public void deleteReviews(int productId) {

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
}
