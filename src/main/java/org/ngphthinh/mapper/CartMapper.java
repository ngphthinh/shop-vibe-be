package org.ngphthinh.mapper;

import org.mapstruct.Mapper;
import org.ngphthinh.dto.response.cart.CartItemResponse;
import org.ngphthinh.dto.response.cart.CartResponse;
import org.ngphthinh.dto.response.product.ProductResponse;
import org.ngphthinh.entity.CartItem;
import org.ngphthinh.repository.projection.CartProjection;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CartMapper {


    default CartResponse toCartResponse(List<CartProjection> cartProjection) {
        if (cartProjection == null || cartProjection.isEmpty()) {
            return CartResponse.builder()
                    .id(null)
                    .totalAmount(BigDecimal.ZERO)
                    .totalItems(0)
                    .items(List.of())
                    .build();
        }

        // Đọc thông tin tổng quan của Giỏ hàng từ phần tử đầu tiên
        CartProjection firstElement = cartProjection.get(0);
        Long cartId = firstElement.getId();
        BigDecimal totalAmount = firstElement.getTotalAmount();
        Integer totalItems = firstElement.getTotalItems();

        if (firstElement.getItemId() == null) {
            return CartResponse.builder()
                    .id(cartId)
                    .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                    .totalItems(totalItems != null ? totalItems : 0)
                    .items(List.of())
                    .build();
        }

        // Nếu thực sự có items, tiến hành map danh sách
        List<CartItemResponse> items = cartProjection.stream()
                .map(this::toCartItemResponse)
                .toList();

        return CartResponse.builder()
                .id(cartId)
                .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .totalItems(totalItems != null ? totalItems : 0)
                .items(items)
                .build();
    }

    default CartItemResponse toCartItemResponse(CartProjection cartProjection) {
        if (cartProjection == null) {
            return null;
        }

        ProductResponse productResponse = ProductResponse.builder()
                .id(cartProjection.getProductId())
                .name(cartProjection.getProductName())
                .price(cartProjection.getProductPrice())
                .primaryImageUrl(cartProjection.getProductPrimaryImageUrl())
                .build();

        return CartItemResponse.builder()
                .id(cartProjection.getItemId())
                .product(productResponse)
                .quantity(cartProjection.getQuantity())
                .unitPrice(cartProjection.getUnitPrice())
                .subtotal(cartProjection.getSubtotal())
                .build();
    }

    default CartItemResponse toCartItemResponse(CartItem cartItem) {
        if (cartItem == null) {
            return null;
        }

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .subtotal(cartItem.getSubtotal())
                .build();

    }
}

