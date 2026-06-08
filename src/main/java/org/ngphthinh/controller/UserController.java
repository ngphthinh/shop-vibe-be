package org.ngphthinh.controller;

import lombok.RequiredArgsConstructor;
import org.ngphthinh.dto.request.user.UserUpdateRequest;
import org.ngphthinh.dto.response.ApiResponse;
import org.ngphthinh.dto.response.PagingResponse;
import org.ngphthinh.dto.response.user.UserResponse;
import org.ngphthinh.enums.ResponseCode;
import org.ngphthinh.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser() {
        return ApiResponse.<UserResponse>builder()
                .code(ResponseCode.USER_RETRIEVED_SUCCESSFULLY.getCode())
                .message(ResponseCode.USER_RETRIEVED_SUCCESSFULLY.getMessage())
                .data(userService.getMyProfile())
                .build();
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateCurrentUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        return ApiResponse.<UserResponse>builder()
                .code(ResponseCode.USER_UPDATED_SUCCESSFULLY.getCode())
                .message(ResponseCode.USER_UPDATED_SUCCESSFULLY.getMessage())
                .data(userService.updateUserProfile(userUpdateRequest))
                .build();
    }

    @GetMapping("/admin/all")
    public ApiResponse<PagingResponse<UserResponse>> getAllUsers(@PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.<PagingResponse<UserResponse>>builder()
                .code(ResponseCode.USERS_RETRIEVED_SUCCESSFULLY.getCode())
                .message(ResponseCode.USERS_RETRIEVED_SUCCESSFULLY.getMessage())
                .data(userService.getAllUsers(pageable))
                .build();
    }

    @PutMapping("/admin/{id}/lock")
    public ApiResponse<Void> lockUser(@PathVariable String id) {
        userService.lockUser(id);
        return ApiResponse.<Void>builder()
                .code(ResponseCode.USER_LOCKED_SUCCESSFULLY.getCode())
                .message(ResponseCode.USER_LOCKED_SUCCESSFULLY.getMessage())
                .build();
    }

    @PutMapping("/admin/{id}/unlock")
    public ApiResponse<Void> unlockUser(@PathVariable String id) {
        userService.unlockUser(id);
        return ApiResponse.<Void>builder()
                .code(ResponseCode.USER_UNLOCKED_SUCCESSFULLY.getCode())
                .message(ResponseCode.USER_UNLOCKED_SUCCESSFULLY.getMessage())
                .build();
    }

}
