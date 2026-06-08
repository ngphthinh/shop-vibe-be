package org.ngphthinh.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ngphthinh.dto.request.cart.CartAddRequest;
import org.ngphthinh.dto.request.cart.CartItemUpdateRequest;
import org.ngphthinh.dto.response.ApiResponse;
import org.ngphthinh.dto.response.cart.CartItemResponse;
import org.ngphthinh.dto.response.cart.CartResponse;
import org.ngphthinh.dto.response.cart.CartUpdateResponse;
import org.ngphthinh.enums.ResponseCode;
import org.ngphthinh.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> getCart() {
        return ApiResponse.<CartResponse>builder()
                .code(ResponseCode.CART_RETRIEVE_SUCCESS.getCode())
                .message(ResponseCode.CART_RETRIEVE_SUCCESS.getMessage())
                .data(cartService.getCartItems())
                .build();
    }

    @PostMapping("/items")
    public ApiResponse<CartItemResponse> addCartItem(@RequestBody CartAddRequest request) {
        return ApiResponse.<CartItemResponse>builder()
                .code(ResponseCode.CART_ITEM_ADD_SUCCESS.getCode())
                .message(ResponseCode.CART_ITEM_ADD_SUCCESS.getMessage())
                .data(cartService.addCartItem(request))
                .build();
    }

    @PutMapping("/items/{cartItemId}")
    public ApiResponse<CartUpdateResponse> updateCartItem(@PathVariable Long cartItemId, @Valid @RequestBody CartItemUpdateRequest request) {
        return ApiResponse.<CartUpdateResponse>builder()
                .code(ResponseCode.CART_ITEM_UPDATE_SUCCESS.getCode())
                .message(ResponseCode.CART_ITEM_UPDATE_SUCCESS.getMessage())
                .data(cartService.updateCartItems(cartItemId, request))
                .build();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<Void> deleteCartItem(@PathVariable Long cartItemId) {
        cartService.removeCartItem(cartItemId);
        return ApiResponse.<Void>builder()
                .code(ResponseCode.CART_ITEM_DELETE_SUCCESS.getCode())
                .message(ResponseCode.CART_ITEM_DELETE_SUCCESS.getMessage())
                .build();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/clear")
    public ApiResponse<Void> clearCart() {
        cartService.clearCart();
        return ApiResponse.<Void>builder()
                .code(ResponseCode.CART_CLEAR_SUCCESS.getCode())
                .message(ResponseCode.CART_CLEAR_SUCCESS.getMessage())
                .build();
    }
}
