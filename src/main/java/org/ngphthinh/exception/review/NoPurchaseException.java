package org.ngphthinh.exception.review;

import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;

public class NoPurchaseException extends AppException {
    public NoPurchaseException() {
        super(ErrorCode.REVIEW_NO_PURCHASE);
    }
}
