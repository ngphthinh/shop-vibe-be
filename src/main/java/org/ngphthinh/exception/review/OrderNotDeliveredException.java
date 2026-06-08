package org.ngphthinh.exception.review;


import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;

public class OrderNotDeliveredException extends AppException {
    public OrderNotDeliveredException() {
        super(ErrorCode.ORDER_NOT_DELIVERED);
    }
}
