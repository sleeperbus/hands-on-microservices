package se.magnus.microservices.core.recommendation.services;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.microservices.core.recommendation.persistent.RecommendationEntity;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RecommendationMapper {

     @Mapping(target = "id", ignore = true)
     @Mapping(target = "version", ignore = true)
     @Mapping(target = "rating", source = "rate")
     RecommendationEntity apiToEntity(Recommendation api);

     @Mapping(target = "rate", source = "rating")
     @Mapping(target = "serviceAddress", ignore = true)
     Recommendation entityToApi(RecommendationEntity entity);

     List<Recommendation> entryListToApiList(List<RecommendationEntity> entryList);
}
