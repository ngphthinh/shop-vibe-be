package org.ngphthinh.exception.auth;

import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;

public class TokenExpiredException extends AppException {
    public TokenExpiredException() {
        super(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}
