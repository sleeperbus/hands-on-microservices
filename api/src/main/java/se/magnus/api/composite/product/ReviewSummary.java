package se.magnus.api.composite.product;

import lombok.Getter;

@Getter
public class ReviewSummary {
    private final int reviewId;
    private final String author;
    private final String content;

    public ReviewSummary(int reviewId, String author, String content) {
        this.reviewId = reviewId;
        this.author = author;
        this.content = content;
    }
}
