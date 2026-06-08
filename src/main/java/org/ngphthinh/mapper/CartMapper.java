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
        if (cartProjection == null) {
            return null;
        }

        if (cartProjection.isEmpty()) {
            return CartResponse.builder()
                    .id(null)
                    .totalAmount(BigDecimal.ZERO)
                    .totalItems(0)
                    .items(List.of())
                    .build();
        }

        Long cartId = cartProjection.get(0).getId();
        BigDecimal totalAmount = cartProjection.get(0).getTotalAmount();
        Integer totalItems = cartProjection.get(0).getTotalItems();

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

