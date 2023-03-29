package se.magnus.microservices.core.review;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import se.magnus.microservices.core.review.persistent.ReviewEntity;
import se.magnus.microservices.core.review.persistent.ReviewRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PersistenceTests {
    @Autowired
    private ReviewRepository repository;
    private ReviewEntity savedEntity;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        ReviewEntity entity = new ReviewEntity(1, 2, "a", "s", "c");
        savedEntity = repository.save(entity);

        assertEqualsReview(entity, savedEntity);
    }

    @Test
    public void create() {
        ReviewEntity newEntity = new ReviewEntity(1, 3, "a", "s", "c");
        repository.save(newEntity);

        ReviewEntity foundEntity = repository.findById(newEntity.getId()).get();
        assertEqualsReview(newEntity, foundEntity);
        assertEquals(2, repository.count());
    }

    @Test
    public void update() {
        savedEntity.setAuthor("a2");
        repository.save(savedEntity);

        ReviewEntity foundEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, foundEntity.getVersion());
        assertEquals("a2", foundEntity.getAuthor());
    }

    @Test
    public void delete() {
        repository.delete(savedEntity);
        assertFalse(repository.existsById(savedEntity.getId()));
    }

    @Test
    public void getByProductId() {
        List<ReviewEntity> entryList = repository.findByProductId(savedEntity.getProductId());

        assertEquals(1, entryList.size());
        assertEqualsReview(savedEntity, entryList.get(0));
    }

    @Test
    public void duplicateError() {
        assertThrows(DataIntegrityViolationException.class, () -> {
            ReviewEntity entity = new ReviewEntity(1, 2, "a", "s", "c");
            repository.save(entity);
        });
    }

    @Test
    public void optimisticLocking() {
        ReviewEntity entity1 = repository.findById(savedEntity.getId()).get();
        ReviewEntity entity2 = repository.findById(savedEntity.getId()).get();

        entity1.setAuthor("a2");
        repository.save(entity1);

        try {
            entity2.setAuthor("a3");
            repository.save(entity2);
            fail("Fail OptimisticLockingFailureException");

        } catch (OptimisticLockingFailureException e) {}

        ReviewEntity foundEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, foundEntity.getVersion());
        assertEquals("a2", foundEntity.getAuthor());
    }

    private void assertEqualsReview(ReviewEntity expected, ReviewEntity actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getVersion(), actual.getVersion());
        assertEquals(expected.getProductId(), actual.getProductId());
        assertEquals(expected.getReviewId(), actual.getReviewId());
        assertEquals(expected.getAuthor(), actual.getAuthor());
        assertEquals(expected.getSubject(), actual.getSubject());
        assertEquals(expected.getContent(), actual.getContent());
    }
}
