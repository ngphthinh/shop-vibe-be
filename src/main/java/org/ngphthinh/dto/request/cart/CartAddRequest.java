package org.ngphthinh.dto.request.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Request object for adding a product to the cart")
public class CartAddRequest {
    @Schema(description = "ID of the product to add to the cart", example = "1")
    private Long productId;
    @Schema(description = "Quantity of the product to add to the cart", example = "2")
    private Integer quantity;
}
