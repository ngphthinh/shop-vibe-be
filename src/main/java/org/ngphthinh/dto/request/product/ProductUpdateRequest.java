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
@Schema(description = "Request object for updating a product - all fields are optional, only provide the fields that need to be updated")
public class ProductUpdateRequest {
    @Schema(description = "Name of the product", example = "Smartphone XYZ")
    private String name;
    @Schema(description = "Description of the product", example = "A high-performance smartphone with advanced features")
    private String description;
    @Schema(description = "Price of the product", example = "999.99")
    private BigDecimal price;
    @Schema(description = "Stock quantity of the product", example = "100")
    private Integer stockQuantity;
    @Schema(description = "ID of the category to which the product belongs", example = "1")
    private Long categoryId;
}
