package org.ngphthinh.util;

import jakarta.servlet.http.HttpServletRequest;
import org.ngphthinh.dto.response.ErrorResponse;
import org.ngphthinh.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public class AppUtil {
    public static ErrorResponse generateErrorResponse(HttpServletRequest request, ErrorCode errorCode) {
        return ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(errorCode.getStatusCode().value())
                .error(HttpStatus.valueOf(errorCode.getStatusCode().value()).getReasonPhrase())
                .message(errorCode.getMessage())
                .path(request.getRequestURI())
                .build();
    }
}
