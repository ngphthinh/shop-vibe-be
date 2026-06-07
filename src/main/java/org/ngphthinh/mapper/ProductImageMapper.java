package org.ngphthinh.mapper;

import org.mapstruct.Mapper;
import org.ngphthinh.dto.response.product.ProductImageResponse;
import org.ngphthinh.entity.ProductImage;

@Mapper(componentModel = "spring")
public interface ProductImageMapper {
    ProductImageResponse toProductImageResponse(ProductImage productImage);
}

