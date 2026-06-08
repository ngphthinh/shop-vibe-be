package org.ngphthinh.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.ngphthinh.dto.response.cart.CartItemResponse;
import org.ngphthinh.entity.OrderItem;
import org.ngphthinh.entity.Product;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", source = "product") // Map nguyên object Product vào thuộc tính product của OrderItem
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productThumbnail", source = "productThumbnail")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unitPrice", source = "product.price")
    OrderItem toOrderItem(Product product, int quantity, String productThumbnail);

}

