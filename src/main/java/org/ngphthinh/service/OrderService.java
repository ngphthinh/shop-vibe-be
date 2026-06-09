package org.ngphthinh.service;

import lombok.RequiredArgsConstructor;
import org.ngphthinh.dto.request.order.OrderCancelRequest;
import org.ngphthinh.dto.request.order.OrderCreateRequest;
import org.ngphthinh.dto.request.order.OrderUpdateStatusRequest;
import org.ngphthinh.dto.response.PagingResponse;
import org.ngphthinh.dto.response.cart.CartItemResponse;
import org.ngphthinh.dto.response.cart.CartResponse;
import org.ngphthinh.dto.response.order.OrderResponse;
import org.ngphthinh.entity.*;
import org.ngphthinh.enums.OrderStatus;
import org.ngphthinh.enums.PaymentStatus;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;
import org.ngphthinh.exception.cart.EmptyCartException;
import org.ngphthinh.exception.order.InvalidStatusTransitionException;
import org.ngphthinh.exception.order.OrderCannotBeCancelledException;
import org.ngphthinh.exception.product.InsufficientStockException;
import org.ngphthinh.mapper.OrderItemMapper;
import org.ngphthinh.mapper.OrderMapper;
import org.ngphthinh.repository.OrderRepository;
import org.ngphthinh.repository.ProductRepository;
import org.ngphthinh.repository.UserRepository;
import org.ngphthinh.repository.projection.OrderProjection;
import org.ngphthinh.util.AppUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class OrderService {

    private final CartService cartService;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final SecureRandom secureRandom;


    @PreAuthorize("hasRole('USER')")
    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {

        CartResponse cartItems = cartService.getCartItems(); // Kiểm tra xem cart có tồn tại và có items hay không, nếu không sẽ ném exception

        if (cartItems.getItems().isEmpty()) {
            throw new EmptyCartException();
        }
        Order order = orderMapper.toOrder(request);
        order.setItems(new HashSet<>());
        // Kiểm tra tồn kho của từng sản phẩm trong cart, nếu có sản phẩm nào không đủ tồn kho sẽ ném exception
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItemResponse item : cartItems.getItems()) {
            Product product = productRepository.findByIdAndIsDeletedFalse(item.getProduct().getId()).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new InsufficientStockException("name", product.getName(), "availableStock", product.getStockQuantity());
            }
            OrderItem orderItem = orderItemMapper.toOrderItem(product, item.getQuantity(), item.getProduct().getPrimaryImageUrl());
            order.addItem(orderItem);
            orderItem.setSubtotal(orderItem.getUnitPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));
            // Cộng dồn tổng tiền của đơn hàng
            totalAmount = totalAmount.add(orderItem.getSubtotal());
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity()); // Cập nhật lại tồn kho của sản phẩm

        }

        order.setStatus(OrderStatus.PENDING);
        order.setOrderCode(generateOrderCode());
        order.setTotalAmount(totalAmount);
        Payment payment = Payment.builder()
                .id(null)
                .amount(cartItems.getTotalAmount())
                .method(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .order(order)
                .paidAt(null)
                .transactionId(null)
                .build();

        order.setPayment(payment);

        // Get user from email and set to order
        String userEmail = AppUtil.emailFromAuthentication();
        User user = userRepository.getReferenceById(userRepository.findIdByEmail(userEmail).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));
        order.setUser(user);

        cartService.clearCart();
        //Todo: Send notification to seller
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    public String generateOrderCode() {
        String prefix = "ORD";
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));


        int randomNumber = 1000 + secureRandom.nextInt(9000);

        return String.format("%s-%s-%s", prefix, date, randomNumber);
        // → ORD-20240601-4827
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public PagingResponse<OrderResponse> getOrders(Pageable pageable, String status, LocalDate from, LocalDate to) {
        try {
            String email = AppUtil.emailFromAuthentication();


            OrderStatus orderStatus;
            if (status == null || status.isEmpty()) {
                orderStatus = null;
            } else {
                orderStatus = OrderStatus.valueOf(status.toUpperCase());
            }


            Page<OrderProjection> orders = orderRepository.findByUserEmailAndStatusAndCreatedAtBetween(email, orderStatus, from.atStartOfDay(), to.atTime(LocalTime.MAX), pageable);

            return PagingResponse.<OrderResponse>builder()
                    .content(orderMapper.mapToOrderResponses(orders.getContent()))
                    .page(orders.getNumber())
                    .size(orders.getSize())
                    .totalElements(orders.getTotalElements())
                    .totalPages(orders.getTotalPages())
                    .last(orders.isLast())
                    .build();

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public OrderResponse getOrderById(Long orderId) {
        String email = AppUtil.emailFromAuthentication();
        List<OrderProjection> orders = orderRepository.findProjectionByIdAndUserEmail(orderId, email);
        if (orders.isEmpty()) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        return orderMapper.toOrderResponse(orders);
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public OrderResponse cancelOrder(Long orderId, OrderCancelRequest request) {
        String email = AppUtil.emailFromAuthentication();
        Order order = orderRepository.findByIdAndUserEmail(orderId, email).orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getEmail().equals(email)) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderCannotBeCancelledException();
        }

        handleCancelOrder(order);
        order.setCancelReason(request.getReason());
        order.setStatus(OrderStatus.CANCELLED);

        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    private void handleCancelOrder(Order order) {
        order.getItems().forEach(item -> {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity()); // Hoàn trả lại tồn kho của sản phẩm
            productRepository.save(product);
        });

        if (order.getPayment() != null && order.getPayment().getStatus().equals(PaymentStatus.SUCCESS)) {
            order.getPayment().setStatus(PaymentStatus.REFUNDED);
        }

        order.setCancelledAt(LocalDateTime.now());
    }


    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public PagingResponse<OrderResponse> getAllOrders(Pageable pageable, String status, LocalDate from, LocalDate to) {
        OrderStatus orderStatus;
        if (status == null || status.isEmpty()) {
            orderStatus = null;
        } else {
            try {
                orderStatus = OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
            }
        }

        Page<OrderProjection> orders = orderRepository.findByStatusAndCreatedAtBetween(orderStatus, from.atStartOfDay(), to.atTime(LocalTime.MAX), pageable);

        return PagingResponse.<OrderResponse>builder()
                .content(orderMapper.mapToOrderResponses(orders.getContent()))
                .page(orders.getNumber())
                .size(orders.getSize())
                .totalElements(orders.getTotalElements())
                .totalPages(orders.getTotalPages())
                .last(orders.isLast())
                .build();

    }


    @Transactional
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public OrderResponse updateOrderStatus(Long orderId, OrderUpdateStatusRequest request) {

        // 1. Định nghĩa các bước chuyển trạng thái hợp lệ
        final Map<OrderStatus, List<OrderStatus>> VALID_TRANSITIONS = Map.of(
                OrderStatus.PENDING, List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
                OrderStatus.CONFIRMED, List.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED),
                OrderStatus.SHIPPING, List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED),
                OrderStatus.DELIVERED, List.of(),
                OrderStatus.CANCELLED, List.of()
        );

        // 2. Tìm đơn hàng
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new AppException(ErrorCode.ORDER_STATUS_UPDATE_FORBIDDEN);
        }

        // 3. Parse và kiểm tra tính hợp lệ của Enum nhập vào
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(request.getNewStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 4. Kiểm tra logic chuyển đổi trạng thái
        OrderStatus currentStatus = order.getStatus();
        if (!VALID_TRANSITIONS.get(currentStatus).contains(newStatus)) {
            throw new InvalidStatusTransitionException("oldStatus", currentStatus.name(), "newStatus", newStatus.name());
        }

        // 5. Xử lý các nghiệp vụ đặc thù (hoàn tiền, cộng/trừ kho,...)
        // Nếu các method này ném ra RuntimeException, Spring sẽ TỰ ĐỘNG ROLLBACK toàn bộ câu lệnh SQL phía trên.
        switch (newStatus) {
            case CANCELLED -> handleCancelOrder(order);
            case DELIVERED -> handleDelivered(order);
        }

        // 6. Cập nhật trạng thái mới
        order.setStatus(newStatus);

        return orderMapper.toOrderResponse(order);
    }

    private void handleDelivered(Order order) {
        Payment payment = order.getPayment();
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());

    }
}