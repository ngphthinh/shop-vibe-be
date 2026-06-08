package org.ngphthinh.dto.response.cart;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Inheritance;
import lombok.*;
import org.ngphthinh.dto.response.product.ProductResponse;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class CartItemResponse implements CartUpdateResponse{
    private Long id;
    private ProductResponse product;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

}
