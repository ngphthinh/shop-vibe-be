package org.ngphthinh.util;

import org.ngphthinh.dto.response.cart.CartMessageUpdateResponse;

public class MessageResponse {
    public static CartMessageUpdateResponse of(String message) {
        return CartMessageUpdateResponse.builder()
                .message(message)
                .build();
    }
}
