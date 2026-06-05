package org.ngphthinh.enums;

import lombok.Getter;

@Getter
public enum ResponseCode {


    AUTH_REGISTER_SUCCESS(1000, "Successfully registered"),

    AUTH_LOGIN_SUCCESS(1001, "Successfully logged in"),

    AUTH_INTROSPECT_SUCCESS(1002, "Successfully introspected"),

    AUTH_CHANGE_PASSWORD_SUCCESS(1003, "Successfully changed password"),

    AUTH_LOGOUT_SUCCESS(1004, "Successfully logged out"),
    ;
    private final int code;
    private final String message;

    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }


}
