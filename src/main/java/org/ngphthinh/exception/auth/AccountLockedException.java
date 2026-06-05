package org.ngphthinh.exception.auth;

import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;

public class AccountLockedException extends AppException {
    public AccountLockedException() {
        super(ErrorCode.ACCOUNT_LOCKED);
    }
}
