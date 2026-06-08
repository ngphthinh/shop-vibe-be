package org.ngphthinh.exception.auth;

import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;

public class ForbiddenException extends AppException {
    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }
}
