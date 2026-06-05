package org.ngphthinh.exception.auth;

import lombok.Getter;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;

@Getter
public class DuplicateEmailException extends AppException {

    private final String keyAttribute;
    private final String attributeValue;


    public DuplicateEmailException(String keyAttribute, String attributeValue) {
        super(ErrorCode.EMAIL_ALREADY_EXISTS);
        this.keyAttribute = keyAttribute;
        this.attributeValue = attributeValue;
    }
}
