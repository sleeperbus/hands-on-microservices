package se.magnus.microservices.core.review.services;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import se.magnus.api.core.review.Review;
import se.magnus.microservices.core.review.persistent.ReviewEntity;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2023-04-02T21:24:27+0900",
    comments = "version: 1.5.3.Final, compiler: javac, environment: Java 17.0.4 (Amazon.com Inc.)"
)
@Component
public class ReviewMapperImpl implements ReviewMapper {

    @Override
    public ReviewEntity apiToEntity(Review body) {
        if ( body == null ) {
            return null;
        }

        ReviewEntity reviewEntity = new ReviewEntity();

        reviewEntity.setProductId( body.getProductId() );
        reviewEntity.setReviewId( body.getReviewId() );
        reviewEntity.setAuthor( body.getAuthor() );
        reviewEntity.setSubject( body.getSubject() );
        reviewEntity.setContent( body.getContent() );

        return reviewEntity;
    }

    @Override
    public Review entityToApi(ReviewEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Review review = new Review();

        review.setProductId( entity.getProductId() );
        review.setReviewId( entity.getReviewId() );
        review.setAuthor( entity.getAuthor() );
        review.setSubject( entity.getSubject() );
        review.setContent( entity.getContent() );

        return review;
    }

    @Override
    public List<Review> entityListToApiList(List<ReviewEntity> entityList) {
        if ( entityList == null ) {
            return null;
        }

        List<Review> list = new ArrayList<Review>( entityList.size() );
        for ( ReviewEntity reviewEntity : entityList ) {
            list.add( entityToApi( reviewEntity ) );
        }

        return list;
    }
}
