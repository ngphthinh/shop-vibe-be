package org.ngphthinh.controller;

import jakarta.servlet.annotation.HttpConstraint;
import lombok.RequiredArgsConstructor;
import org.ngphthinh.dto.request.review.ReviewRequest;
import org.ngphthinh.dto.response.ApiResponse;
import org.ngphthinh.dto.response.PagingResponse;
import org.ngphthinh.dto.response.review.ReviewResponse;
import org.ngphthinh.enums.ResponseCode;
import org.ngphthinh.service.ReviewService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor

@RestController
@RequestMapping("/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/products/{productId}")
    public ApiResponse<PagingResponse<ReviewResponse>> getReviewsByProductId(
            @PathVariable Long productId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<PagingResponse<ReviewResponse>>builder()
                .code(ResponseCode.REVIEWS_RETRIEVED_SUCCESSFULLY.getCode())
                .message(ResponseCode.REVIEWS_RETRIEVED_SUCCESSFULLY.getMessage())
                .data(reviewService.getReviewsByProductId(productId, pageable))
                .build();

    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/products/{productId}")
    public ApiResponse<ReviewResponse> createReview(
            @PathVariable Long productId,
            @RequestBody ReviewRequest reviewRequest) {
        return ApiResponse.<ReviewResponse>builder()
                .code(ResponseCode.REVIEW_CREATED_SUCCESSFULLY.getCode())
                .message(ResponseCode.REVIEW_CREATED_SUCCESSFULLY.getMessage())
                .data(reviewService.createReview(productId, reviewRequest))
                .build();
    }

    @PutMapping("/{reviewId}")
    public ApiResponse<ReviewResponse> updateReview(@PathVariable Long reviewId, @RequestBody ReviewRequest reviewRequest) {
        return ApiResponse.<ReviewResponse>builder()
                .code(ResponseCode.REVIEW_UPDATED_SUCCESSFULLY.getCode())
                .message(ResponseCode.REVIEW_UPDATED_SUCCESSFULLY.getMessage())
                .data(reviewService.updateReview(reviewId, reviewRequest))
                .build();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{reviewId}")
    public ApiResponse<ReviewResponse> deleteReview(@PathVariable Long reviewId) {
        return ApiResponse.<ReviewResponse>builder()
                .code(ResponseCode.REVIEW_DELETED_SUCCESSFULLY.getCode())
                .message(ResponseCode.REVIEW_DELETED_SUCCESSFULLY.getMessage())
                .data(reviewService.deleteReview(reviewId))
                .build();
    }
}
