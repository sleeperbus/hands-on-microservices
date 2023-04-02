package se.magnus.microservices.core.review.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.microservices.core.review.persistent.ReviewEntity;
import se.magnus.microservices.core.review.persistent.ReviewRepository;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.http.ServiceUtil;

import java.util.List;

@RestController
public class ReviewServiceImpl implements ReviewService {
    private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);
    private final ServiceUtil serviceUtil;
    private final ReviewRepository repository;
    private final ReviewMapper mapper;

    public ReviewServiceImpl(ServiceUtil serviceUtil, ReviewRepository repository, ReviewMapper mapper) {
        this.serviceUtil = serviceUtil;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Review> getReviews(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        List<ReviewEntity> entryList = repository.findByProductId(productId);
        List<Review> reviews = mapper.entityListToApiList(entryList);
        reviews.forEach(r -> r.setServiceAddress(serviceUtil.getServiceAddress()));

        return reviews;
    }

    @Override
    public Review createReview(Review body) {
        try {
            ReviewEntity entity = mapper.apiToEntity(body);
            ReviewEntity newEntity = repository.save(entity);

            LOG.debug("createView: created a review entity: {}/{}", body.getProductId(), body.getReviewId());
            return mapper.entityToApi(newEntity);
        } catch (DataIntegrityViolationException div) {
            throw new InvalidInputException("Duplicate key, product Id: " + body.getProductId() + ", Review Id: " + body.getReviewId());
        }
    }

    @Override
    public void deleteReviews(int productId) {
        LOG.debug("deleteReviews: tries to delete reviews for the product with productId: {}", productId);
        repository.deleteAll(repository.findByProductId(productId));
    }
}
