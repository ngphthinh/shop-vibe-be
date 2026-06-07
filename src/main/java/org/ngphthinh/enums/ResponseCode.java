package org.ngphthinh.enums;

import lombok.Getter;

@Getter
public enum ResponseCode {


    AUTH_REGISTER_SUCCESS(1000, "Successfully registered"),

    AUTH_LOGIN_SUCCESS(1001, "Successfully logged in"),

    AUTH_INTROSPECT_SUCCESS(1002, "Successfully introspected"),

    AUTH_CHANGE_PASSWORD_SUCCESS(1003, "Successfully changed password"),

    AUTH_LOGOUT_SUCCESS(1004, "Successfully logged out"),

    CATEGORY_GET_SUCCESS(2000, "Successfully retrieved categories"),
    CATEGORY_GET_BY_ID_SUCCESS(2001, "Successfully retrieved category by ID"),
    CATEGORY_CREATE_SUCCESS(2002, "Successfully created category"),
    CATEGORY_UPDATE_SUCCESS(2003, "Successfully updated category"),

    PRODUCT_GET_SUCCESS(3000, "Successfully retrieved products"),
    PRODUCT_GET_BY_ID_SUCCESS(3001, "Successfully retrieved product by ID"),
    PRODUCT_CREATE_SUCCESS(3002, "Successfully created product"),
    PRODUCT_UPDATE_SUCCESS(3003, "Successfully updated product"),
    PRODUCT_DELETE_SUCCESS(3004, "Successfully deleted product");

    private final int code;
    private final String message;

    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }


}
