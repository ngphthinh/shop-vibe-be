package org.ngphthinh.dto.response.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public final class CartMessageUpdateResponse implements CartUpdateResponse {
    private String message;
}
