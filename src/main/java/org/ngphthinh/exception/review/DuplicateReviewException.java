package org.ngphthinh.exception.review;

import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;

public class DuplicateReviewException extends AppException {

    public DuplicateReviewException() {
        super(ErrorCode.REVIEW_ALREADY_EXISTS);
    }
}
