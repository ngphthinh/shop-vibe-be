package org.ngphthinh.exception.auth;

import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;

public class DuplicateEmailException extends AppException {

    public DuplicateEmailException() {
        super(ErrorCode.EMAIL_ALREADY_EXISTS);
    }
}
