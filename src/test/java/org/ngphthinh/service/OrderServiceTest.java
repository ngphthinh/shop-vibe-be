package org.ngphthinh.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ngphthinh.dto.request.order.OrderCancelRequest;
import org.ngphthinh.dto.request.order.OrderCreateRequest;
import org.ngphthinh.dto.request.order.OrderUpdateStatusRequest;
import org.ngphthinh.dto.response.cart.CartItemResponse;
import org.ngphthinh.dto.response.cart.CartResponse;
import org.ngphthinh.dto.response.order.OrderResponse;
import org.ngphthinh.dto.response.product.ProductResponse;
import org.ngphthinh.entity.*;
import org.ngphthinh.enums.OrderStatus;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.cart.EmptyCartException;
import org.ngphthinh.exception.order.InvalidStatusTransitionException;
import org.ngphthinh.exception.order.OrderCannotBeCancelledException;
import org.ngphthinh.exception.product.InsufficientStockException;
import org.ngphthinh.mapper.OrderItemMapper;
import org.ngphthinh.mapper.OrderMapper;
import org.ngphthinh.repository.OrderRepository;
import org.ngphthinh.repository.ProductRepository;
import org.ngphthinh.repository.UserRepository;
import org.ngphthinh.util.AppUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private CartService cartService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderMapper orderMapper;


    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemMapper orderItemMapper;

    @InjectMocks
    private OrderService orderService;

    private User mockUser;
    private Product mockProduct;
    private Order mockOrder;
    private ProductResponse mockProductResponse;
    private CartItemResponse cartItem;


    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("Test User")
                .build();

        mockProduct = Product.builder()
                .id(1L)
                .name("Product A")
                .price(BigDecimal.valueOf(100))
                .stockQuantity(10)
                .isDeleted(false)
                .build();

        mockOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20240601-1234")
                .user(mockUser)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(300))
                .shippingAddress("123 Main St")
                .build();

        mockProductResponse =
                org.ngphthinh.dto.response.product.ProductResponse.builder()
                        .id(1L) // ID này phải khớp với ID product bạn dùng để mock ở dưới (Optional.of(mockProduct))
                        .name("Product A")
                        .price(BigDecimal.valueOf(100))
                        .build();

        // 2. Cập nhật lại giỏ hàng trong đoạn test của bạn
        cartItem = CartItemResponse.builder()
                .id(1L)
                .product(mockProductResponse)
                .quantity(3)
                .unitPrice(BigDecimal.valueOf(100))
                .subtotal(BigDecimal.valueOf(300))
                .build();
    }

    // ===== CREATE ORDER TESTS =====

    @Test
    public void createOrder_success_shouldCreateOrderAndDeductStock() {
        try (MockedStatic<AppUtil> utilities = mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("user@example.com");

            // 1. DÙNG LUÔN biến 'cartItem' (đã có product) lấy từ setUp(), KHÔNG khai báo lại bằng từ khóa 'CartItemResponse'
            cartItem = CartItemResponse.builder()
                    .id(1L)
                    .product(mockProductResponse) // 🔥 Giữ nguyên đối tượng product để tránh NPE tại item.getProduct().getId()
                    .quantity(3)
                    .unitPrice(BigDecimal.valueOf(100))
                    .subtotal(BigDecimal.valueOf(300))
                    .build();

            CartResponse cartResponse = CartResponse.builder()
                    .id(1L)
                    .items(List.of(cartItem))
                    .totalAmount(BigDecimal.valueOf(300))
                    .totalItems(3)
                    .build();

            // 2. Định nghĩa các hành vi Mock (Stubbing)
            when(cartService.getCartItems()).thenReturn(cartResponse);
            when(productRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(mockProduct));

            // Giả lập map từ request sang Order entity trống ban đầu
            when(orderMapper.toOrder(any(OrderCreateRequest.class))).thenReturn(new Order());

            // 🔥 GIẢ LẬP ORDER ITEM MAPPER (Nếu không có dòng này, biến orderItem sẽ bị null)
            OrderItem mockOrderItem = OrderItem.builder()
                    .product(mockProduct)
                    .quantity(3)
                    .unitPrice(BigDecimal.valueOf(100))
                    .build();
            when(orderItemMapper.toOrderItem(any(), anyInt(), any())).thenReturn(mockOrderItem);

            when(userRepository.findIdByEmail("user@example.com")).thenReturn(Optional.of(1L));
            when(userRepository.getReferenceById(1L)).thenReturn(mockUser);

            mockProduct.setStockQuantity(10);
            OrderCreateRequest request = OrderCreateRequest.builder()
                    .paymentMethod("COD")
                    .shippingAddress("123 Main St")
                    .build();

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(1L);
                order.setCreatedAt(LocalDateTime.now());
                return order;
            });

            when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(OrderResponse.builder()
                    .id(1L)
                    .orderCode("ORD-20240601-1234")
                    .status("PENDING")
                    .totalAmount(BigDecimal.valueOf(300))
                    .build());

            doNothing().when(cartService).clearCart();

            // 3. Thực thi hàm test
            OrderResponse response = orderService.createOrder(request);

            // 4. Kiểm chứng kết quả (Assertions)
            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("PENDING", response.getStatus());
            assertEquals(BigDecimal.valueOf(300), response.getTotalAmount());
            assertEquals(7, mockProduct.getStockQuantity()); // Kiểm tra xem tồn kho có bị trừ từ 10 xuống 7 không

            verify(cartService).clearCart();
            verify(orderRepository).save(any(Order.class));
        }
    }

    @Test
    public void createOrder_emptyCart_shouldThrowEmptyCartException() {
        try (MockedStatic<AppUtil> utilities = mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("user@example.com");

            CartResponse emptyCart = CartResponse.builder()
                    .id(1L)
                    .items(Collections.emptyList())
                    .totalAmount(BigDecimal.ZERO)
                    .totalItems(0)
                    .build();

            when(cartService.getCartItems()).thenReturn(emptyCart);

            OrderCreateRequest request = OrderCreateRequest.builder()
                    .paymentMethod("COD")
                    .shippingAddress("123 Main St")
                    .build();

            assertThrows(EmptyCartException.class, () -> orderService.createOrder(request));
        }
    }

    @Test
    public void createOrder_insufficientStock_shouldThrowInsufficientStockException() {
        try (MockedStatic<AppUtil> utilities = mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("user@example.com");

            // 1. Khắc phục NPE: Gắn đối tượng product giả lập vào cartItem để qua được dòng item.getProduct().getId()
            CartItemResponse cartItem = CartItemResponse.builder()
                    .id(1L)
                    .product(mockProductResponse)
                    .quantity(3) // Số lượng mua là 3
                    .unitPrice(BigDecimal.valueOf(100))
                    .subtotal(BigDecimal.valueOf(300))
                    .build();

            CartResponse cartResponse = CartResponse.builder()
                    .id(1L)
                    .items(List.of(cartItem))
                    .totalAmount(BigDecimal.valueOf(300))
                    .totalItems(3)
                    .build();

            when(cartService.getCartItems()).thenReturn(cartResponse);

            // Giả lập tồn kho chỉ còn 2 (nhỏ hơn số lượng mua là 3)
            mockProduct.setStockQuantity(2);

            // 2. Khớp đúng phương thức Repository được gọi thực tế trong tầng Service
            when(productRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(mockProduct));

            // 3. Mock thêm tầng mapper chuyển đổi request thành thực thể trống ban đầu
            when(orderMapper.toOrder(any(OrderCreateRequest.class))).thenReturn(new Order());

            OrderCreateRequest request = OrderCreateRequest.builder()
                    .paymentMethod("COD")
                    .shippingAddress("123 Main St")
                    .build();

            // Kiểm chứng: Thực thi hàm và kỳ vọng hệ thống sẽ chặn lại, ném ra đúng InsufficientStockException
            assertThrows(InsufficientStockException.class, () -> orderService.createOrder(request));
        }
    }

    @Test
    public void createOrder_stockDeducted_shouldReduceStockQuantity() {
        try (MockedStatic<AppUtil> utilities = mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("user@example.com");

            // 1. Sửa lỗi NPE: Thêm .product(mockProductResponse) vào cartItem
            CartItemResponse cartItem = CartItemResponse.builder()
                    .id(1L)
                    .product(mockProductResponse)
                    .quantity(3)
                    .unitPrice(BigDecimal.valueOf(100))
                    .subtotal(BigDecimal.valueOf(300))
                    .build();

            CartResponse cartResponse = CartResponse.builder()
                    .id(1L)
                    .items(List.of(cartItem))
                    .totalAmount(BigDecimal.valueOf(300))
                    .totalItems(3)
                    .build();

            when(cartService.getCartItems()).thenReturn(cartResponse);
            when(userRepository.findIdByEmail("user@example.com")).thenReturn(Optional.of(1L));
            when(userRepository.getReferenceById(1L)).thenReturn(mockUser);

            mockProduct.setStockQuantity(10);

            // 2. Sửa lỗi sai hàm repo: Đổi findById thành findByIdAndIsDeletedFalse theo đúng Service
            when(productRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(mockProduct));

            OrderCreateRequest request = OrderCreateRequest.builder()
                    .paymentMethod("COD")
                    .shippingAddress("123 Main St")
                    .build();

            // 3. Mock thêm các Mapper cần thiết
            when(orderMapper.toOrder(any(OrderCreateRequest.class))).thenReturn(new Order());

            OrderItem mockOrderItem = OrderItem.builder()
                    .product(mockProduct)
                    .quantity(3)
                    .unitPrice(BigDecimal.valueOf(100))
                    .build();
            when(orderItemMapper.toOrderItem(any(), anyInt(), any())).thenReturn(mockOrderItem);

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(1L);
                return order;
            });

            when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(OrderResponse.builder()
                    .id(1L)
                    .status("PENDING")
                    .totalAmount(BigDecimal.valueOf(300))
                    .build());

            doNothing().when(cartService).clearCart();

            // Thực thi
            orderService.createOrder(request);

            // Kiểm chứng
            assertEquals(7, mockProduct.getStockQuantity());
            verify(orderRepository).save(any(Order.class));
        }
    }

    @Test
    public void createOrder_cartCleared_shouldClearUserCart() {
        try (MockedStatic<AppUtil> utilities = mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("user@example.com");

            // 1. Khắc phục NPE: Gắn đối tượng product giả lập vào cartItem
            CartItemResponse cartItem = CartItemResponse.builder()
                    .id(1L)
                    .product(mockProductResponse) // 🔥 Thêm dòng này
                    .quantity(1)
                    .unitPrice(BigDecimal.valueOf(100))
                    .subtotal(BigDecimal.valueOf(100))
                    .build();

            CartResponse cartResponse = CartResponse.builder()
                    .id(1L)
                    .items(List.of(cartItem))
                    .totalAmount(BigDecimal.valueOf(100))
                    .totalItems(1)
                    .build();

            when(cartService.getCartItems()).thenReturn(cartResponse);
            when(userRepository.findIdByEmail("user@example.com")).thenReturn(Optional.of(1L));
            when(userRepository.getReferenceById(1L)).thenReturn(mockUser);

            // 2. Khớp đúng phương thức Repository được gọi trong tầng Service
            when(productRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(mockProduct));

            OrderCreateRequest request = OrderCreateRequest.builder()
                    .paymentMethod("COD")
                    .shippingAddress("123 Main St")
                    .build();

            // 3. Khai báo hành vi mock cho các Mapper
            when(orderMapper.toOrder(any(OrderCreateRequest.class))).thenReturn(new Order());

            OrderItem mockOrderItem = OrderItem.builder()
                    .product(mockProduct)
                    .quantity(1)
                    .unitPrice(BigDecimal.valueOf(100))
                    .build();
            when(orderItemMapper.toOrderItem(any(), anyInt(), any())).thenReturn(mockOrderItem);

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(1L);
                return order;
            });

            when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(OrderResponse.builder()
                    .id(1L)
                    .status("PENDING")
                    .build());

            doNothing().when(cartService).clearCart();

            // Thực thi hàm cần test
            orderService.createOrder(request);

            // Kiểm chứng xem giỏ hàng đã được xóa sạch sau khi đặt đơn thành công chưa
            verify(cartService).clearCart();
        }
    }

    // ===== CANCEL ORDER TESTS =====

    @Test
    public void cancelOrder_success_shouldCancelPendingOrder() {
        try (MockedStatic<AppUtil> utilities = mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("user@example.com");

            OrderItem item = OrderItem.builder()
                    .id(1L)
                    .product(mockProduct)
                    .quantity(2)
                    .unitPrice(BigDecimal.valueOf(100))
                    .subtotal(BigDecimal.valueOf(200))
                    .build();

            mockOrder.setItems(Set.of(item));
            mockOrder.setStatus(OrderStatus.PENDING);

            when(orderRepository.findByIdAndUserEmail(1L, "user@example.com")).thenReturn(Optional.of(mockOrder));

            OrderCancelRequest request = OrderCancelRequest.builder()
                    .reason("Changed my mind")
                    .build();

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                return order;
            });

            when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(OrderResponse.builder()
                    .id(1L)
                    .status("CANCELLED")
                    .build());

            OrderResponse response = orderService.cancelOrder(1L, request);

            assertNotNull(response);
            assertEquals("CANCELLED", response.getStatus());
            verify(orderRepository).save(any(Order.class));
        }
    }

    @Test
    public void cancelOrder_notPending_shouldThrowOrderCannotBeCancelledException() {
        try (MockedStatic<AppUtil> utilities = mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("user@example.com");

            mockOrder.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findByIdAndUserEmail(1L, "user@example.com")).thenReturn(Optional.of(mockOrder));

            OrderCancelRequest request = OrderCancelRequest.builder()
                    .reason("Changed my mind")
                    .build();

            assertThrows(OrderCannotBeCancelledException.class, () -> orderService.cancelOrder(1L, request));
        }
    }

    @Test
    public void cancelOrder_notOwner_shouldThrowAppException() {
        try (MockedStatic<AppUtil> utilities = mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("other@example.com");

            when(orderRepository.findByIdAndUserEmail(1L, "other@example.com")).thenReturn(Optional.empty());

            OrderCancelRequest request = OrderCancelRequest.builder()
                    .reason("Changed my mind")
                    .build();

            assertThrows(AppException.class, () -> orderService.cancelOrder(1L, request));
        }
    }

    // ===== UPDATE STATUS TESTS =====

    @Test
    public void updateOrderStatus_validTransition_shouldUpdateStatusSuccessfully() {
        mockOrder.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        OrderUpdateStatusRequest request = OrderUpdateStatusRequest.builder()
                .newStatus("CONFIRMED")
                .build();


        when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(OrderResponse.builder()
                .id(1L)
                .status("CONFIRMED")
                .build());

        OrderResponse response = orderService.updateOrderStatus(1L, request);

        assertNotNull(response);
        assertEquals("CONFIRMED", response.getStatus());
        assertEquals(OrderStatus.CONFIRMED, mockOrder.getStatus());
    }

    @Test
    public void updateOrderStatus_invalidTransition_shouldThrowInvalidStatusTransitionException() {
        mockOrder.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        OrderUpdateStatusRequest request = OrderUpdateStatusRequest.builder()
                .newStatus("SHIPPING")
                .build();

        assertThrows(InvalidStatusTransitionException.class, () -> orderService.updateOrderStatus(1L, request));
    }
}
