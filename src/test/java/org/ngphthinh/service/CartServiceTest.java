package org.ngphthinh.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ngphthinh.dto.request.cart.CartAddRequest;
import org.ngphthinh.dto.request.cart.CartItemUpdateRequest;
import org.ngphthinh.dto.response.cart.CartMessageUpdateResponse;
import org.ngphthinh.dto.response.cart.CartItemResponse;
import org.ngphthinh.entity.Cart;
import org.ngphthinh.entity.CartItem;
import org.ngphthinh.entity.Product;
import org.ngphthinh.entity.User;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;
import org.ngphthinh.mapper.CartMapper;
import org.ngphthinh.repository.CartItemRepository;
import org.ngphthinh.repository.CartRepository;
import org.ngphthinh.repository.ProductRepository;
import org.ngphthinh.util.AppUtil;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    public void addCartItem_whenCartNotFound_shouldThrow() {
        try (MockedStatic<AppUtil> utilities = Mockito.mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("no-cart@example.com");

            when(cartRepository.findIdByUserEmail("no-cart@example.com")).thenReturn(Optional.empty());

            CartAddRequest req = CartAddRequest.builder().productId(1L).quantity(1).build();

            AppException ex = assertThrows(AppException.class, () -> cartService.addCartItem(req));
            assertEquals(ErrorCode.CART_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Test
    public void addCartItem_whenNewItem_shouldSaveAndReturn() {
        try (MockedStatic<AppUtil> utilities = Mockito.mockStatic(AppUtil.class)) {
            String email = "user@example.com";
            utilities.when(AppUtil::emailFromAuthentication).thenReturn(email);

            Long cartId = 10L;
            when(cartRepository.findIdByUserEmail(email)).thenReturn(Optional.of(cartId));

            Cart cart = new Cart();
            cart.setId(cartId);
            when(cartRepository.getReferenceById(cartId)).thenReturn(cart);

            Product product = new Product();
            product.setId(2L);
            product.setPrice(BigDecimal.valueOf(100));
            when(productRepository.findById(2L)).thenReturn(Optional.of(product));

            when(cartItemRepository.findByProductIdAndCartId(2L, cartId)).thenReturn(Optional.empty());

            // mock save to set id
            when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
                CartItem arg = invocation.getArgument(0);
                arg.setId(55L);
                return arg;
            });

            when(cartMapper.toCartItemResponse(any(CartItem.class))).thenAnswer(invocation -> {
                CartItem ci = invocation.getArgument(0);
                return CartItemResponse.builder()
                        .id(ci.getId())
                        .quantity(ci.getQuantity())
                        .unitPrice(ci.getUnitPrice())
                        .subtotal(ci.getSubtotal())
                        .build();
            });

            doNothing().when(cartRepository).updateTotalAmountAndTotalItems(cartId);

            CartAddRequest req = CartAddRequest.builder().productId(2L).quantity(3).build();

            CartItemResponse resp = cartService.addCartItem(req);

            assertNotNull(resp);
            assertEquals(55L, resp.getId());
            assertEquals(3, resp.getQuantity());
            assertEquals(BigDecimal.valueOf(100), resp.getUnitPrice());
            assertEquals(BigDecimal.valueOf(300), resp.getSubtotal());

            verify(cartItemRepository).save(any(CartItem.class));
            verify(cartRepository).updateTotalAmountAndTotalItems(cartId);
        }
    }

    @Test
    public void updateCartItems_whenCartItemNotFound_shouldThrow() {
        try (MockedStatic<AppUtil> utilities = Mockito.mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("user@example.com");

            when(cartItemRepository.findById(99L)).thenReturn(Optional.empty());

            CartItemUpdateRequest req = CartItemUpdateRequest.builder().quantity(1).build();

            AppException ex = assertThrows(AppException.class, () -> cartService.updateCartItems(99L, req));
            assertEquals(ErrorCode.CART_ITEM_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Test
    public void updateCartItems_whenNotOwner_shouldThrow() {
        try (MockedStatic<AppUtil> utilities = Mockito.mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("user@example.com");

            // build cartItem owned by another user
            Product product = new Product();
            product.setId(2L);
            product.setStockQuantity(10);

            CartItem cartItem = new CartItem();
            Cart cart = new Cart();
            User other = new User();
            other.setEmail("other@example.com");
            cart.setUser(other);
            cart.setId(5L);
            cartItem.setCart(cart);
            cartItem.setProduct(product);

            when(cartItemRepository.findById(77L)).thenReturn(Optional.of(cartItem));

            CartItemUpdateRequest req = CartItemUpdateRequest.builder().quantity(1).build();

            AppException ex = assertThrows(AppException.class, () -> cartService.updateCartItems(77L, req));
            assertEquals(ErrorCode.CART_ITEM_NOT_BELONG_TO_USER, ex.getErrorCode());
        }
    }

    @Test
    public void updateCartItems_whenQuantityZero_shouldDeleteAndReturnMessage() {
        try (MockedStatic<AppUtil> utilities = Mockito.mockStatic(AppUtil.class)) {
            String email = "user@example.com";
            utilities.when(AppUtil::emailFromAuthentication).thenReturn(email);

            Product product = new Product();
            product.setId(2L);
            product.setStockQuantity(10);

            CartItem cartItem = new CartItem();
            Cart cart = new Cart();
            User user = new User();
            user.setEmail(email);
            cart.setUser(user);
            cart.setId(11L);
            cartItem.setCart(cart);
            cartItem.setProduct(product);

            when(cartItemRepository.findById(33L)).thenReturn(Optional.of(cartItem));

            doNothing().when(cartItemRepository).delete(cartItem);
            doNothing().when(cartRepository).updateTotalAmountAndTotalItems(11L);

            CartItemUpdateRequest req = CartItemUpdateRequest.builder().quantity(0).build();

            var resp = cartService.updateCartItems(33L, req);

            assertTrue(resp instanceof CartMessageUpdateResponse);
            CartMessageUpdateResponse msg = (CartMessageUpdateResponse) resp;
            assertEquals("Cart item removed successfully", msg.getMessage());

            verify(cartItemRepository).delete(cartItem);
            verify(cartRepository).updateTotalAmountAndTotalItems(11L);
        }
    }

    @Test
    public void updateCartItems_whenInsufficientStock_shouldThrow() {
        try (MockedStatic<AppUtil> utilities = Mockito.mockStatic(AppUtil.class)) {
            String email = "user@example.com";
            utilities.when(AppUtil::emailFromAuthentication).thenReturn(email);

            Product product = new Product();
            product.setId(2L);
            product.setStockQuantity(2);

            CartItem cartItem = new CartItem();
            Cart cart = new Cart();
            User user = new User();
            user.setEmail(email);
            cart.setUser(user);
            cart.setId(11L);
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setUnitPrice(BigDecimal.valueOf(100));
            cartItem.setQuantity(1);

            when(cartItemRepository.findById(44L)).thenReturn(Optional.of(cartItem));

            CartItemUpdateRequest req = CartItemUpdateRequest.builder().quantity(5).build();

            AppException ex = assertThrows(AppException.class, () -> cartService.updateCartItems(44L, req));
            assertEquals(ErrorCode.INSUFFICIENT_PRODUCT_STOCK, ex.getErrorCode());
        }
    }

    @Test
    public void updateCartItems_whenUpdateSuccessful_shouldSaveAndReturn() {
        try (MockedStatic<AppUtil> utilities = Mockito.mockStatic(AppUtil.class)) {
            String email = "user@example.com";
            utilities.when(AppUtil::emailFromAuthentication).thenReturn(email);

            Product product = new Product();
            product.setId(2L);
            product.setStockQuantity(10);

            CartItem cartItem = new CartItem();
            Cart cart = new Cart();
            User user = new User();
            user.setEmail(email);
            cart.setUser(user);
            cart.setId(15L);
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setUnitPrice(BigDecimal.valueOf(100));
            cartItem.setQuantity(1);

            when(cartItemRepository.findById(55L)).thenReturn(Optional.of(cartItem));

            when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            when(cartMapper.toCartItemResponse(any(CartItem.class))).thenAnswer(invocation -> {
                CartItem ci = invocation.getArgument(0);
                return CartItemResponse.builder()
                        .id(ci.getId())
                        .quantity(ci.getQuantity())
                        .unitPrice(ci.getUnitPrice())
                        .subtotal(ci.getSubtotal())
                        .build();
            });

            doNothing().when(cartRepository).updateTotalAmountAndTotalItems(15L);

            CartItemUpdateRequest req = CartItemUpdateRequest.builder().quantity(4).build();

            CartItemResponse resp = (CartItemResponse) cartService.updateCartItems(55L, req);

            assertNotNull(resp);
            assertEquals(4, resp.getQuantity());
            assertEquals(BigDecimal.valueOf(100), resp.getUnitPrice());
            assertEquals(BigDecimal.valueOf(400), resp.getSubtotal());

            verify(cartItemRepository).save(any(CartItem.class));
            verify(cartRepository).updateTotalAmountAndTotalItems(15L);
        }
    }

    @Test
    public void removeCartItem_whenCartItemNotFound_shouldThrow() {
        try (MockedStatic<AppUtil> utilities = Mockito.mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("user@example.com");

            when(cartItemRepository.findById(101L)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> cartService.removeCartItem(101L));
            assertEquals(ErrorCode.CART_ITEM_NOT_FOUND, ex.getErrorCode());

            verify(cartItemRepository, never()).delete(any(CartItem.class));
            verify(cartRepository, never()).updateTotalAmountAndTotalItems(anyLong());
        }
    }

    @Test
    public void removeCartItem_whenNotOwner_shouldThrow() {
        try (MockedStatic<AppUtil> utilities = Mockito.mockStatic(AppUtil.class)) {
            utilities.when(AppUtil::emailFromAuthentication).thenReturn("user@example.com");

            Cart cart = new Cart();
            cart.setId(5L);
            User other = new User();
            other.setEmail("other@example.com");
            cart.setUser(other);

            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);

            when(cartItemRepository.findById(102L)).thenReturn(Optional.of(cartItem));

            AppException ex = assertThrows(AppException.class, () -> cartService.removeCartItem(102L));
            assertEquals(ErrorCode.CART_ITEM_NOT_BELONG_TO_USER, ex.getErrorCode());

            verify(cartItemRepository, never()).delete(any(CartItem.class));
            verify(cartRepository, never()).updateTotalAmountAndTotalItems(anyLong());
        }
    }

    @Test
    public void removeCartItem_whenOwner_shouldDeleteAndUpdateTotals() {
        try (MockedStatic<AppUtil> utilities = Mockito.mockStatic(AppUtil.class)) {
            String email = "user@example.com";
            utilities.when(AppUtil::emailFromAuthentication).thenReturn(email);

            Cart cart = new Cart();
            cart.setId(7L);
            User user = new User();
            user.setEmail(email);
            cart.setUser(user);

            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);

            when(cartItemRepository.findById(103L)).thenReturn(Optional.of(cartItem));
            doNothing().when(cartItemRepository).delete(cartItem);
            doNothing().when(cartRepository).updateTotalAmountAndTotalItems(7L);

            cartService.removeCartItem(103L);

            verify(cartItemRepository).delete(cartItem);
            verify(cartRepository).updateTotalAmountAndTotalItems(7L);
        }
    }

    @Test
    public void clearCart_whenCartNotFound_shouldThrow() {
        try (MockedStatic<AppUtil> utilities = Mockito.mockStatic(AppUtil.class)) {
            String email = "missing-cart@example.com";
            utilities.when(AppUtil::emailFromAuthentication).thenReturn(email);

            when(cartRepository.findIdByUserEmail(email)).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> cartService.clearCart());
            assertEquals(ErrorCode.CART_NOT_FOUND, ex.getErrorCode());

            verify(cartItemRepository, never()).deleteAllByCartId(anyLong());
            verify(cartRepository, never()).updateTotalAmountAndTotalItems(anyLong());
        }
    }

    @Test
    public void clearCart_whenCartExists_shouldDeleteAllAndUpdateTotals() {
        try (MockedStatic<AppUtil> utilities = Mockito.mockStatic(AppUtil.class)) {
            String email = "user@example.com";
            Long cartId = 88L;
            utilities.when(AppUtil::emailFromAuthentication).thenReturn(email);

            when(cartRepository.findIdByUserEmail(email)).thenReturn(Optional.of(cartId));
            doNothing().when(cartItemRepository).deleteAllByCartId(cartId);
            doNothing().when(cartRepository).updateTotalAmountAndTotalItems(cartId);

            cartService.clearCart();

            verify(cartItemRepository).deleteAllByCartId(cartId);
            verify(cartRepository).updateTotalAmountAndTotalItems(cartId);
        }
    }


}

