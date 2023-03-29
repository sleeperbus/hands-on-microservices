package se.magnus.microservices.core.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import se.magnus.microservices.core.product.persistent.ProductEntity;
import se.magnus.microservices.core.product.persistent.ProductRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static java.util.stream.IntStream.rangeClosed;
import static org.springframework.data.domain.Sort.Direction.ASC;

@DataMongoTest()
@ExtendWith(SpringExtension.class)
class PersistenceTests {
    @Autowired
    private ProductRepository repository;
    private ProductEntity savedEntity;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        ProductEntity entity = new ProductEntity(1, "n", 1);
        savedEntity = repository.save(entity);
        assertEqualProduct(entity, savedEntity);
    }

    @Test
    void create() {
        ProductEntity newEntity = new ProductEntity(2, "n", 2);
        savedEntity = repository.save(newEntity);

        ProductEntity foundEntity = repository.findById(newEntity.getId()).get();
        assertEqualProduct(newEntity, foundEntity);

        assertEquals(2, repository.count());
    }

    @Test
    void update() {
        savedEntity.setName("n2");
        repository.save(savedEntity);

        ProductEntity foundEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, foundEntity.getVersion());
        assertEquals("n2", foundEntity.getName());
    }

    @Test
    void delete() {
        repository.delete(savedEntity);
        assertFalse(repository.existsById(savedEntity.getId()));
    }

    @Test
    public void getByProductId() {
        Optional<ProductEntity> entity = repository.findByProductId(savedEntity.getProductId());
        assertTrue(entity.isPresent());
        assertEqualProduct(savedEntity, entity.get());
    }

    @Test
    public void duplicateError() {
        assertThrows(DuplicateKeyException.class, () -> {
            ProductEntity entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
            repository.save(entity);
        });
    }

    @Test
    public void optimisticLockError() {
        ProductEntity entity1 = repository.findById(savedEntity.getId()).get();
        ProductEntity entity2 = repository.findById(savedEntity.getId()).get();

        entity1.setName("n1");
        repository.save(entity1);

        try {
            entity2.setName("n2");
            repository.save(entity2);
            fail("Expected an OptimisticLockingFailureException");
        } catch (OptimisticLockingFailureException e ) {}

        ProductEntity updatedEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, updatedEntity.getVersion());
        assertEquals("n1", updatedEntity.getName());
    }

    @Test
    public void paging() {
        repository.deleteAll();
        List<ProductEntity> newProducts = rangeClosed(1001, 1010).mapToObj(i -> new ProductEntity(i, "name " + i, i)).toList();
        repository.saveAll(newProducts);

        Pageable nextPage = (Pageable) PageRequest.of(0, 4, ASC, "productId");

        nextPage = testNextPage(nextPage, "[1001, 1002, 1003, 1004]", true);
        nextPage = testNextPage(nextPage, "[1005, 1006, 1007, 1008]", true);
        nextPage = testNextPage(nextPage, "[1009, 1010]", false);
    }

    private Pageable testNextPage(Pageable nextPage, String expectedProductsId, boolean expectsNextPage) {
        Page<ProductEntity> productPage = repository.findAll(nextPage);
        assertEquals(expectedProductsId, productPage.getContent().stream().map(ProductEntity::getProductId).toList().toString());
        assertEquals(expectsNextPage, productPage.hasNext());
        return productPage.nextPageable();
    }


    private void assertEqualProduct(ProductEntity expectedEntity, ProductEntity actualEntity) {
        assertEquals(expectedEntity.getId(), actualEntity.getId());
        assertEquals(expectedEntity.getVersion(), actualEntity.getVersion());
        assertEquals(expectedEntity.getProductId(), actualEntity.getProductId());
        assertEquals(expectedEntity.getName(), actualEntity.getName());
        assertEquals(expectedEntity.getWeight(), actualEntity.getWeight());
    }
}
