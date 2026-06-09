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
    PRODUCT_DELETE_SUCCESS(3004, "Successfully deleted product"),

    CART_ITEM_ADD_SUCCESS(4000, "Successfully added item to cart"),
    CART_RETRIEVE_SUCCESS(4000, "Successfully retrieved cart"),
    CART_ITEM_UPDATE_SUCCESS(4001, "Successfully updated cart item"),
    CART_ITEM_DELETE_SUCCESS(4002, "Successfully deleted cart item"),
    CART_CLEAR_SUCCESS(4003, "Successfully cleared cart"),

    ORDER_CREATE_SUCCESS(5000, "Successfully created order"),
    ORDER_GET_SUCCESS(5001, "Successfully retrieved orders"),
    ORDER_GET_BY_ID_SUCCESS(5002, "Successfully retrieved order by ID"),
    ORDER_CANCEL_SUCCESS(5003, "Successfully cancelled order"),
    ORDER_STATUS_UPDATE_SUCCESS(5004, "Successfully updated order status"),

    REVIEWS_RETRIEVED_SUCCESSFULLY(6000, "Successfully retrieved reviews"),
    REVIEW_CREATED_SUCCESSFULLY(6001, "Successfully created review"),
    REVIEW_UPDATED_SUCCESSFULLY(6002, "Successfully updated review"),
    REVIEW_DELETED_SUCCESSFULLY(6003, "Successfully deleted review"),

    USER_RETRIEVED_SUCCESSFULLY(7000, "Successfully retrieved user information"),
    USER_UPDATED_SUCCESSFULLY(7001, "Successfully updated user information"),
    USERS_RETRIEVED_SUCCESSFULLY(7002, "Successfully retrieved users"),
    USER_LOCKED_SUCCESSFULLY(7003, "Successfully locked user account"),
    USER_UNLOCKED_SUCCESSFULLY(7004, "Successfully unlocked user account"),
    STATISTICS_REVENUE_SUCCESS(8000, "Successfully retrieved revenue statistics"),
    STATISTICS_TOP_PRODUCTS_SUCCESS(8001, "Successfully retrieved top products statistics"),
    STATISTICS_TOP_CUSTOMERS_SUCCESS(8002, "Successfully retrieved top customers statistics"),
    STATISTICS_OVERVIEW_SUCCESS(8003, "Successfully retrieved overview statistics")
    ;

    private final int code;
    private final String message;

    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }


}
