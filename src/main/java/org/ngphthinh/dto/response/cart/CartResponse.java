package org.ngphthinh.dto.response.cart;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartResponse {
    private Long id;
    private BigDecimal totalAmount;
    private Integer totalItems;
    private List<CartItemResponse> items;


}
