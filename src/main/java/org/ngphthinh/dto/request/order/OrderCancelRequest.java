package org.ngphthinh.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(description = "Request to cancel an order")
public class OrderCancelRequest {

    @Schema(description = "Reason for canceling the order", example = "Changed my mind")
    private String reason;

}
