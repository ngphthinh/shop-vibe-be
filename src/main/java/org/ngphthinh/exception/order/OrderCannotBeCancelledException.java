package org.ngphthinh.exception.order;

import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;

public class OrderCannotBeCancelledException extends AppException {
    public OrderCannotBeCancelledException() {
        super(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
    }
}
