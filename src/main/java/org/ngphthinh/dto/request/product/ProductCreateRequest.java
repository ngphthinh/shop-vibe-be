package org.ngphthinh.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(description = "Request body for creating a new product")
public class ProductCreateRequest {
    @Schema(description = "Name of the product", example = "Wireless Mouse")
    private String name;
    @Schema(description = "URL-friendly slug for the product", example = "wireless-mouse")
    private String description;
    @Schema(description = "Price of the product", example = "299.00")
    private BigDecimal price;
    @Schema(description = "Stock quantity of the product", example = "50")
    private Integer stockQuantity;
    @Schema(description = "ID of the category to which the product belongs", example = "3")
    private Long categoryId;
}
