package org.ngphthinh.exception.product;

import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;

public class InsufficientStockException extends AppException {
    public InsufficientStockException() {
        super(ErrorCode.INSUFFICIENT_PRODUCT_STOCK);
    }
}
