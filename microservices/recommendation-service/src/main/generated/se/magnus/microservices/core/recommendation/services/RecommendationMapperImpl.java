package se.magnus.microservices.core.recommendation.services;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.microservices.core.recommendation.persistent.RecommendationEntity;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2023-04-15T11:21:42+0900",
    comments = "version: 1.5.3.Final, compiler: javac, environment: Java 17.0.4 (Amazon.com Inc.)"
)
@Component
public class RecommendationMapperImpl implements RecommendationMapper {

    @Override
    public RecommendationEntity apiToEntity(Recommendation api) {
        if ( api == null ) {
            return null;
        }

        RecommendationEntity recommendationEntity = new RecommendationEntity();

        recommendationEntity.setRating( api.getRate() );
        recommendationEntity.setProductId( api.getProductId() );
        recommendationEntity.setRecommendationId( api.getRecommendationId() );
        recommendationEntity.setAuthor( api.getAuthor() );
        recommendationEntity.setContent( api.getContent() );

        return recommendationEntity;
    }

    @Override
    public Recommendation entityToApi(RecommendationEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Recommendation recommendation = new Recommendation();

        recommendation.setRate( entity.getRating() );
        recommendation.setProductId( entity.getProductId() );
        recommendation.setRecommendationId( entity.getRecommendationId() );
        recommendation.setAuthor( entity.getAuthor() );
        recommendation.setContent( entity.getContent() );

        return recommendation;
    }

    @Override
    public List<Recommendation> entryListToApiList(List<RecommendationEntity> entryList) {
        if ( entryList == null ) {
            return null;
        }

        List<Recommendation> list = new ArrayList<Recommendation>( entryList.size() );
        for ( RecommendationEntity recommendationEntity : entryList ) {
            list.add( entityToApi( recommendationEntity ) );
        }

        return list;
    }
}
