package org.ngphthinh.service;

import lombok.RequiredArgsConstructor;
import org.ngphthinh.dto.request.cart.CartAddRequest;
import org.ngphthinh.dto.request.cart.CartItemUpdateRequest;
import org.ngphthinh.dto.response.PagingResponse;
import org.ngphthinh.dto.response.cart.CartItemResponse;
import org.ngphthinh.dto.response.cart.CartResponse;
import org.ngphthinh.dto.response.cart.CartUpdateResponse;
import org.ngphthinh.entity.Cart;
import org.ngphthinh.entity.CartItem;
import org.ngphthinh.entity.Product;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;
import org.ngphthinh.mapper.CartMapper;
import org.ngphthinh.repository.CartItemRepository;
import org.ngphthinh.repository.CartRepository;
import org.ngphthinh.repository.ProductRepository;
import org.ngphthinh.util.AppUtil;
import org.ngphthinh.util.MessageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Service
public class CartService {

    private final CartRepository cartRepository;

    private final CartMapper cartMapper;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;


    @PreAuthorize("hasRole('USER')")
    public CartResponse getCartItems() {
        String email = AppUtil.emailFromAuthentication();


        return cartMapper.toCartResponse(cartRepository.findByUserEmail(email));
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public CartItemResponse addCartItem(CartAddRequest request) {

        String email = AppUtil.emailFromAuthentication();

        // Lấy cartId của người dùng từ email để tạo Proxy object, tránh việc truy vấn toàn bộ cart và items
        Long cartId = cartRepository.findIdByUserEmail(email).orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));
        Cart cart = cartRepository.getReferenceById(cartId);


        Product product = productRepository.findById(request.getProductId()).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        CartItem cartItem = cartItemRepository.findByProductIdAndCartId(request.getProductId(), cartId)
                .map(
                        items -> {
                            items.setQuantity(items.getQuantity() + request.getQuantity());
                            return items;
                        }
                ).orElseGet(() -> CartItem.builder()
                        .cart(cart)
                        .quantity(request.getQuantity())
                        .build());

        cartItem.setUnitPrice(product.getPrice());
        cartItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        cartItem.setProduct(product);
        CartItem cartItemSave = cartItemRepository.save(cartItem);

        cartRepository.updateTotalAmountAndTotalItems(cartId);


        return cartMapper.toCartItemResponse(cartItemSave);
    }

    @PreAuthorize("hasRole('USER')")
    @Transactional
    public CartUpdateResponse updateCartItems(Long cartItemId, CartItemUpdateRequest request) {

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));
        validateCartOwnership(cartItem);


        if (request.getQuantity() == 0) {
            cartItemRepository.delete(cartItem);
            cartRepository.updateTotalAmountAndTotalItems(cartItem.getCart().getId());
            return MessageResponse.of("Cart item removed successfully");
        }

        if (cartItem.getProduct().getStockQuantity() < request.getQuantity()) {
            throw new AppException(ErrorCode.INSUFFICIENT_PRODUCT_STOCK);
        }

        cartItem.setQuantity(request.getQuantity());
        cartItem.setSubtotal(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        CartItem cartItemSave = cartItemRepository.save(cartItem);

        cartRepository.updateTotalAmountAndTotalItems(cartItem.getCart().getId());
        return cartMapper.toCartItemResponse(cartItemSave);
    }


    @Transactional
    @PreAuthorize("hasRole('USER')")
    public void removeCartItem(Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));
        validateCartOwnership(cartItem);

        cartItemRepository.delete(cartItem);
        cartRepository.updateTotalAmountAndTotalItems(cartItem.getCart().getId());
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public void clearCart() {
        String email = AppUtil.emailFromAuthentication();

        Long cartId = cartRepository.findIdByUserEmail(email).orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));
        cartItemRepository.deleteAllByCartId(cartId);
        cartRepository.updateTotalAmountAndTotalItems(cartId);

    }

    private void validateCartOwnership(CartItem cartItem) {
        String email = AppUtil.emailFromAuthentication();
        if (!cartItem.getCart().getUser().getEmail().equals(email)) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_BELONG_TO_USER);
        }
    }

}