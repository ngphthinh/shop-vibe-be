package org.ngphthinh.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrderResponse {
    private Long id;
    private String orderCode;
    private String status;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<OrderItemResponse> items;
    private java.math.BigDecimal totalAmount;
    private String shippingAddress;
    private PaymentResponse payment;
    private String note;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long itemCount;
    private LocalDateTime createdAt;
}
