package org.ngphthinh.service;


import lombok.RequiredArgsConstructor;
import org.ngphthinh.dto.request.review.ReviewRequest;
import org.ngphthinh.dto.response.PagingResponse;
import org.ngphthinh.dto.response.review.ReviewResponse;
import org.ngphthinh.entity.Product;
import org.ngphthinh.entity.Review;
import org.ngphthinh.entity.User;
import org.ngphthinh.enums.OrderStatus;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;
import org.ngphthinh.exception.auth.ForbiddenException;
import org.ngphthinh.exception.review.DuplicateReviewException;
import org.ngphthinh.exception.review.NoPurchaseException;
import org.ngphthinh.exception.review.OrderNotDeliveredException;
import org.ngphthinh.mapper.ReviewMapper;
import org.ngphthinh.repository.OrderRepository;
import org.ngphthinh.repository.ProductRepository;
import org.ngphthinh.repository.ReviewRepository;
import org.ngphthinh.repository.UserRepository;
import org.ngphthinh.util.AppUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public PagingResponse<ReviewResponse> getReviewsByProductId(Long productId, Pageable pageable) {

        Page<Review> reviewPage = reviewRepository.findByProductId(productId, pageable);

        List<ReviewResponse> reviewResponses = reviewPage.getContent().stream()
                .map(reviewMapper::toReviewResponse)
                .toList();

        return PagingResponse.<ReviewResponse>builder()
                .page(reviewPage.getNumber())
                .size(reviewPage.getSize())
                .last(reviewPage.isLast())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .content(reviewResponses)
                .build();
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ReviewResponse createReview(Long productId, ReviewRequest reviewRequest) {
        String email = AppUtil.emailFromAuthentication();

        Long userId = userRepository.findIdByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Each user can only create one review per product
        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new DuplicateReviewException();
        }

        // Check if the user has purchased the product before allowing them to create a review
        if (!orderRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new NoPurchaseException();
        }

        // Check if the user has a delivered order for the product
        if (!orderRepository.existsByUserIdAndProductIdAndStatus(userId, productId, OrderStatus.DELIVERED)) {
            throw new OrderNotDeliveredException();
        }

        Product product = productRepository.getReferenceById(productId);
        User user = userRepository.getReferenceById(userId);

        Review review = reviewMapper.toReview(reviewRequest);
        review.setProduct(product);
        review.setUser(user);

        return reviewMapper.toReviewResponse(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewRequest reviewRequest) {
        Review review = verifyReviewOwner(reviewId);
        reviewMapper.updateReviewFromRequest(reviewRequest, review);

        return reviewMapper.toReviewResponse(review);
    }

    @Transactional
    public ReviewResponse deleteReview(Long reviewId) {
        Review review = verifyReviewOwner(reviewId);
        reviewRepository.delete(review);
        return reviewMapper.toReviewResponse(review);
    }

    private Review verifyReviewOwner(Long reviewId) {
        String email = AppUtil.emailFromAuthentication();

        Long userId = userRepository.findIdByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Review review = reviewRepository.findByIdFetchUser(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getId().equals(userId)) {
            throw new ForbiddenException();
        }
        return review;
    }
}