package org.ngphthinh.exception.auth;

import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;

public class BadCredentialsException extends AppException {
    public BadCredentialsException() {
        super(ErrorCode.INVALID_PASSWORD);
    }
}
