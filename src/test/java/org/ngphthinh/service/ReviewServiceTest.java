package org.ngphthinh.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ngphthinh.dto.request.review.ReviewRequest;
import org.ngphthinh.dto.response.review.ReviewResponse;
import org.ngphthinh.entity.Product;
import org.ngphthinh.entity.Review;
import org.ngphthinh.entity.User;
import org.ngphthinh.enums.OrderStatus;
import org.ngphthinh.exception.AppException;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ReviewMapper reviewMapper;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private ReviewService reviewService;

    private User mockUser;
    private Product mockProduct;
    private Review mockReview;
    private ReviewRequest reviewRequest;
    private ReviewResponse reviewResponse;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).email("user@example.com").fullName("Test User").build();
        mockProduct = Product.builder().id(10L).name("Sản phẩm mẫu").build();

        reviewRequest = ReviewRequest.builder().rating(5).comment("Hàng chuẩn xịn").build();

        mockReview = Review.builder()
                .id(100L).rating(5).comment("Hàng chuẩn xịn").user(mockUser).product(mockProduct)
                .build();

        reviewResponse = ReviewResponse.builder().id(100L).rating(5).comment("Hàng chuẩn xịn").build();
    }

    // =========================================================================
    // 1. CÁC TEST CASES CHO HÀM createReview()
    // =========================================================================

    @Test
    void createReview_success_shouldReturnReviewResponse() {
        try (MockedStatic<AppUtil> utilities = mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("user@example.com");

            when(userRepository.findIdByEmail("user@example.com")).thenReturn(Optional.of(1L));
            when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false);
            when(orderRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(true);
            when(orderRepository.existsByUserIdAndProductIdAndStatus(1L, 10L, OrderStatus.DELIVERED)).thenReturn(true);

            when(productRepository.getReferenceById(10L)).thenReturn(mockProduct);
            when(userRepository.getReferenceById(1L)).thenReturn(mockUser);
            when(reviewMapper.toReview(any(ReviewRequest.class))).thenReturn(mockReview);
            when(reviewRepository.save(any(Review.class))).thenReturn(mockReview);
            when(reviewMapper.toReviewResponse(any(Review.class))).thenReturn(reviewResponse);

            ReviewResponse result = reviewService.createReview(10L, reviewRequest);

            assertNotNull(result);
            assertEquals(100L, result.getId());
            verify(reviewRepository).save(any(Review.class));
        }
    }

    @Test
    void createReview_duplicate_shouldThrowDuplicateReviewException() {
        try (MockedStatic<AppUtil> utilities = mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("user@example.com");

            when(userRepository.findIdByEmail("user@example.com")).thenReturn(Optional.of(1L));
            when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(true); // Đã review

            assertThrows(DuplicateReviewException.class, () -> reviewService.createReview(10L, reviewRequest));
            verify(orderRepository, never()).existsByUserIdAndProductId(any(), any());
        }
    }

    @Test
    void createReview_noPurchase_shouldThrowNoPurchaseException() {
        try (MockedStatic<AppUtil> utilities = mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("user@example.com");

            when(userRepository.findIdByEmail("user@example.com")).thenReturn(Optional.of(1L));
            when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false);
            when(orderRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false); // Chưa từng mua

            assertThrows(NoPurchaseException.class, () -> reviewService.createReview(10L, reviewRequest));
            verify(orderRepository, never()).existsByUserIdAndProductIdAndStatus(any(), any(), any());
        }
    }

    @Test
    void createReview_notDelivered_shouldThrowOrderNotDeliveredException() {
        try (MockedStatic<AppUtil> utilities = mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("user@example.com");

            when(userRepository.findIdByEmail("user@example.com")).thenReturn(Optional.of(1L));
            when(reviewRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(false);
            when(orderRepository.existsByUserIdAndProductId(1L, 10L)).thenReturn(true); // Đã mua
            when(orderRepository.existsByUserIdAndProductIdAndStatus(1L, 10L, OrderStatus.DELIVERED)).thenReturn(false); // Nhưng chưa DELIVERED (Ví dụ: SHIPPING)

            assertThrows(OrderNotDeliveredException.class, () -> reviewService.createReview(10L, reviewRequest));
            verify(reviewRepository, never()).save(any(Review.class));
        }
    }

    // =========================================================================
    // 2. CÁC TEST CASES CHO HÀM updateReview()
    // =========================================================================

    @Test
    void updateReview_success_shouldReturnUpdatedReviewResponse() {
        try (MockedStatic<AppUtil> utilities = mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("user@example.com");

            when(userRepository.findIdByEmail("user@example.com")).thenReturn(Optional.of(1L));
            when(reviewRepository.findByIdFetchUser(100L)).thenReturn(Optional.of(mockReview)); // Owner có ID = 1

            doNothing().when(reviewMapper).updateReviewFromRequest(any(ReviewRequest.class), any(Review.class));
            when(reviewMapper.toReviewResponse(any(Review.class))).thenReturn(reviewResponse);

            ReviewResponse result = reviewService.updateReview(100L, reviewRequest);

            assertNotNull(result);
            verify(reviewMapper).updateReviewFromRequest(reviewRequest, mockReview);
        }
    }

    @Test
    void updateReview_notOwner_shouldThrowForbiddenException() {
        try (MockedStatic<AppUtil> utilities = mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("hacker@example.com");

            when(userRepository.findIdByEmail("hacker@example.com")).thenReturn(Optional.of(99L)); // Người dùng hiện tại có ID = 99
            when(reviewRepository.findByIdFetchUser(100L)).thenReturn(Optional.of(mockReview)); // Nhưng Review thuộc về User ID = 1

            assertThrows(ForbiddenException.class, () -> reviewService.updateReview(100L, reviewRequest));
            verify(reviewRepository, never()).save(any());
        }
    }

    // =========================================================================
    // 3. CÁC TEST CASES CHO HÀM deleteReview()
    // =========================================================================

    @Test
    void deleteReview_success_shouldDeleteAndReturnReviewResponse() {
        try (MockedStatic<AppUtil> utilities = mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("user@example.com");

            when(userRepository.findIdByEmail("user@example.com")).thenReturn(Optional.of(1L));
            when(reviewRepository.findByIdFetchUser(100L)).thenReturn(Optional.of(mockReview));
            doNothing().when(reviewRepository).delete(mockReview);
            when(reviewMapper.toReviewResponse(mockReview)).thenReturn(reviewResponse);

            ReviewResponse result = reviewService.deleteReview(100L);

            assertNotNull(result);
            verify(reviewRepository).delete(mockReview);
        }
    }
}