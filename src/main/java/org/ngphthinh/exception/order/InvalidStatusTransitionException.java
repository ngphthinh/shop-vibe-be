package org.ngphthinh.exception.order;

import lombok.Getter;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;

@Getter
public class InvalidStatusTransitionException extends AppException {
    private final String keyOldStatus;
    private final String attributeOldStatus;

    private final String keyNewStatus;
    private final String attributeNewStatus;


    public InvalidStatusTransitionException(String keyAttribute, String attributeValue, String keyNewStatus, String attributeNewStatus) {
        super(ErrorCode.INVALID_STATUS_TRANSITION);
        this.keyOldStatus = keyAttribute;
        this.attributeOldStatus = attributeValue;
        this.keyNewStatus = keyNewStatus;
        this.attributeNewStatus = attributeNewStatus;
    }

}
