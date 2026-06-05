package org.ngphthinh.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ngphthinh.dto.request.user.*;
import org.ngphthinh.dto.response.ApiResponse;
import org.ngphthinh.dto.response.user.AuthenticateResponse;
import org.ngphthinh.dto.response.user.IntrospectResponse;
import org.ngphthinh.dto.response.user.UserChangePasswordResponse;
import org.ngphthinh.dto.response.user.UserResponse;
import org.ngphthinh.enums.ResponseCode;
import org.ngphthinh.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {


    private final AuthenticationService authenticationService;

    // Response status is 201 Created because we are creating a new user when registering
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = authenticationService.register(request);
        return ApiResponse.<UserResponse>builder()
                .code(ResponseCode.AUTH_REGISTER_SUCCESS.getCode())
                .message(ResponseCode.AUTH_REGISTER_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<AuthenticateResponse> login(@Valid @RequestBody AuthenticateRequest request) {
        AuthenticateResponse response = authenticationService.authenticate(request);
        return ApiResponse.<AuthenticateResponse>builder()
                .code(ResponseCode.AUTH_LOGIN_SUCCESS.getCode())
                .message(ResponseCode.AUTH_LOGIN_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@Valid @RequestBody IntrospectRequest request) {
        IntrospectResponse response = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .code(ResponseCode.AUTH_INTROSPECT_SUCCESS.getCode())
                .message(ResponseCode.AUTH_INTROSPECT_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    @PostMapping("/refresh-token")
    public ApiResponse<AuthenticateResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        AuthenticateResponse response = authenticationService.refreshToken(request);
        return ApiResponse.<AuthenticateResponse>builder()
                .code(ResponseCode.AUTH_LOGIN_SUCCESS.getCode())
                .message(ResponseCode.AUTH_LOGIN_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    @PutMapping("/change-password")
    public ApiResponse<UserChangePasswordResponse> changePassword(@RequestBody UserChangePasswordRequest request) {
        UserChangePasswordResponse response = authenticationService.changePassword(request);
        return ApiResponse.<UserChangePasswordResponse>builder()
                .code(ResponseCode.AUTH_CHANGE_PASSWORD_SUCCESS.getCode())
                .message(ResponseCode.AUTH_CHANGE_PASSWORD_SUCCESS.getMessage())
                .data(response)
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody LogoutRequest request) {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder()
                .code(ResponseCode.AUTH_LOGOUT_SUCCESS.getCode())
                .message(ResponseCode.AUTH_LOGOUT_SUCCESS.getMessage())
                .build();
    }
}
