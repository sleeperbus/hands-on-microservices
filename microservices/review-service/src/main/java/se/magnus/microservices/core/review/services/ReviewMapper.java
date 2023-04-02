package se.magnus.microservices.core.review.services;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import se.magnus.api.core.review.Review;
import se.magnus.microservices.core.review.persistent.ReviewEntity;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReviewMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    ReviewEntity apiToEntity(Review body);

    @Mapping(target = "serviceAddress", ignore = true)
    Review entityToApi(ReviewEntity entity);

    List<Review> entityListToApiList(List<ReviewEntity> entityList);

}
