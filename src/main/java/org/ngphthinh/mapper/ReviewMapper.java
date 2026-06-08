package org.ngphthinh.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.ngphthinh.dto.request.review.ReviewRequest;
import org.ngphthinh.dto.response.review.ReviewResponse;
import org.ngphthinh.entity.Review;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)
public interface ReviewMapper {
    ReviewResponse toReviewResponse(Review review);

    Review toReview(ReviewRequest reviewRequest);


    void updateReviewFromRequest(ReviewRequest reviewRequest, @MappingTarget Review review);
}

