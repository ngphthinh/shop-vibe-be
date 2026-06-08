package org.ngphthinh.exception.cart;

import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;

public class EmptyCartException extends AppException {
    public EmptyCartException() {
        super(ErrorCode.CART_EMPTY);
    }
}
