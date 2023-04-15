package se.magnus.microservices.core.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import se.magnus.microservices.core.recommendation.persistent.RecommendationEntity;
import se.magnus.microservices.core.recommendation.persistent.RecommendationRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@ExtendWith(SpringExtension.class)
class PersistenceTests {
    @Autowired
    private RecommendationRepository repository;
    private RecommendationEntity savedEntity;

    @BeforeEach
    void setupDb() {
        repository.deleteAll().block();
        RecommendationEntity entity = new RecommendationEntity(1, 1, "a", 3, "new book");
        savedEntity = repository.save(entity).block();

        assertEqualsRecommendation(entity, savedEntity);
    }

    private void assertEqualsRecommendation(RecommendationEntity expected, RecommendationEntity actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getVersion(), actual.getVersion());
        assertEquals(expected.getProductId(), actual.getProductId());
        assertEquals(expected.getRecommendationId(), actual.getRecommendationId());
        assertEquals(expected.getAuthor(), actual.getAuthor());
        assertEquals(expected.getRating(), actual.getRating());
        assertEquals(expected.getContent(), actual.getContent());
    }

    @Test
    public void create() {
        RecommendationEntity newEntity = new RecommendationEntity(1, 2, "a", 3, "new book");
        repository.save(newEntity).block();

        RecommendationEntity foundEntity = repository.findById(newEntity.getId()).block();
        assertEqualsRecommendation(newEntity, foundEntity);
        assertEquals(2, repository.count().block());
    }

    @Test
    public void update() {
        savedEntity.setAuthor("b");
        repository.save(savedEntity).block();

        RecommendationEntity foundEntity = repository.findById(savedEntity.getId()).block();
        assertEquals(1, foundEntity.getVersion());
        assertEquals("b", foundEntity.getAuthor());
    }

    @Test
    public void delete() {
        repository.deleteAll().block();
        assertFalse(repository.existsById(savedEntity.getId()).block());
    }

    @Test
    public void getByProductId() {
        List<RecommendationEntity> entityList = repository.findByProductId(savedEntity.getProductId()).collectList().block();
        assertEquals(1, entityList.size());
        assertEqualsRecommendation(savedEntity, entityList.get(0));
    }

    @Test
    public void duplicateError() {
        assertThrows(DuplicateKeyException.class, () -> {
            RecommendationEntity entity = new RecommendationEntity(1, 1, "a", 3, "new book");
            repository.save(entity).block();
        });
    }

    @Test
    public void optimisticLocking() {
        RecommendationEntity entity1 = repository.findById(savedEntity.getId()).block();
        RecommendationEntity entity2 = repository.findById(savedEntity.getId()).block();

        entity1.setAuthor("b");
        repository.save(entity1).block();
        try {
            entity2.setAuthor("c");
            repository.save(entity2).block();
            fail("Expected an OptimisticLockingFailureException");

        } catch (OptimisticLockingFailureException e) {}

        RecommendationEntity updatedEntity = repository.findById(savedEntity.getId()).block();
        assertEquals(1, updatedEntity.getVersion());
        assertEquals("b", updatedEntity.getAuthor());
    }
}
