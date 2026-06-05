package org.ngphthinh.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    EMAIL_ALREADY_EXISTS(409, "Email is already exists", HttpStatus.CONFLICT),
    UNCATEGORIZED_EXCEPTION(500, "Uncategorized exception", HttpStatus.INTERNAL_SERVER_ERROR),
    GENERATE_TOKEN_FAILED(500, "Failed to generate token", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_NOT_FOUND(404, "User not found", HttpStatus.NOT_FOUND),
    INVALID_PASSWORD(401, "Invalid password", HttpStatus.BAD_REQUEST),
    INTROSPECT_FAILED(400, "Failed to introspect token", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(401, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(401, "Invalid refresh token", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(403, "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "Forbidden", HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED(403, "Account locked", HttpStatus.FORBIDDEN);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
