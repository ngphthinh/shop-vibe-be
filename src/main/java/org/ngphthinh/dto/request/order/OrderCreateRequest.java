package org.ngphthinh.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Request object for creating a new order")
public class OrderCreateRequest {
    @Schema(description = "Shipping address for the order", example = "123 Main St, Springfield")
    private String shippingAddress;
    @Schema(description = "Note for the order", example = "Please gift wrap this item")
    private String note;
    @Schema(description = "Payment method for the order", example = "COD")
    private String paymentMethod;
}
