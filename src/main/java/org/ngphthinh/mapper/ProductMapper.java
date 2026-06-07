package org.ngphthinh.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.ngphthinh.dto.request.product.ProductCreateRequest;
import org.ngphthinh.dto.request.product.ProductUpdateRequest;
import org.ngphthinh.dto.response.category.CategoryResponse;
import org.ngphthinh.dto.response.product.ProductDetailResponse;
import org.ngphthinh.dto.response.product.ProductImageResponse;
import org.ngphthinh.dto.response.product.ProductResponse;
import org.ngphthinh.entity.Product;
import org.ngphthinh.repository.projection.ProductImageProjection;
import org.ngphthinh.repository.projection.ProductProjection;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)
public interface ProductMapper {

    default ProductResponse toProductResponse(ProductProjection product) {

        if (product == null) {
            return null;
        }

        CategoryResponse categoryResponse = CategoryResponse.builder()
                .id(product.getCategoryId())
                .name(product.getCategoryName())
                .build();
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .primaryImageUrl(product.getImageUrl())
                .category(categoryResponse)
                .averageRating(product.getAverageRating() != null ? product.getAverageRating() : 0.0)
                .totalReviews(product.getReviewCount() != null ? product.getReviewCount() : 0L)
                .build();
    }

    ProductDetailResponse toProductDetailResponse(Product product);

    default ProductDetailResponse toProductDetailResponse(ProductProjection productProjection, List<ProductImageProjection> imgUrls) {
        if (productProjection == null) {
            return null;
        }

        CategoryResponse categoryResponse = CategoryResponse.builder()
                .id(productProjection.getCategoryId())
                .name(productProjection.getCategoryName())
                .build();

        List<ProductImageResponse> productImageResponse = Collections.emptyList();
        if (imgUrls != null) {
            productImageResponse = imgUrls.stream()
                    .map(img -> ProductImageResponse.builder()
                            .id(img.getId())
                            .imageUrl(img.getImageUrl())
                            .build())
                    .toList();
        }

        return ProductDetailResponse.builder()
                .id(productProjection.getId())
                .name(productProjection.getName())
                .slug(productProjection.getSlug())
                .description(productProjection.getDescription())
                .price(productProjection.getPrice())
                .stockQuantity(productProjection.getStockQuantity())
                .imageUrls(productImageResponse)
                .primaryImageUrl(productProjection.getImageUrl())
                .category(categoryResponse)
                .averageRating(productProjection.getAverageRating() != null ? productProjection.getAverageRating() : 0.0)
                .totalReviews(productProjection.getReviewCount() != null ? productProjection.getReviewCount() : 0L)
                .build();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    Product toProduct(ProductCreateRequest request);

    @Mapping(target = "category", ignore = true)
    void updateProductFromRequest(ProductUpdateRequest request, @MappingTarget Product product);

}
