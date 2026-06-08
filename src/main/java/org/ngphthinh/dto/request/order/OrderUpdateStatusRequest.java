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
@Schema(description = "Request to update the status of an order")
public class OrderUpdateStatusRequest {
    @Schema(description = "New status of the order", example = "CONFIRMED")
    private String newStatus;
}

