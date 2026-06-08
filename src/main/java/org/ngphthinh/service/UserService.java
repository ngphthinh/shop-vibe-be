package org.ngphthinh.service;

import lombok.RequiredArgsConstructor;
import org.ngphthinh.dto.request.user.UserUpdateRequest;
import org.ngphthinh.dto.response.PagingResponse;
import org.ngphthinh.dto.response.user.UserResponse;
import org.ngphthinh.entity.User;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;
import org.ngphthinh.mapper.UserMapper;
import org.ngphthinh.repository.UserRepository;
import org.ngphthinh.util.AppUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void lockAccountDueToMaxFailedAttempts(String email) {
        userRepository.updateIsLockedByEmail(true, false, email);
    }


    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public UserResponse getMyProfile() {
        String email = AppUtil.emailFromAuthentication();
        return userRepository.findByEmail(email)
                .map(userMapper::toUserResponse)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public UserResponse updateUserProfile(UserUpdateRequest request) {
        String email = AppUtil.emailFromAuthentication();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        userMapper.updateUserFromUserResponse(request, user);
        return userMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public PagingResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);

        return PagingResponse.<UserResponse>builder()
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .last(userPage.isLast())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .content(userPage.getContent().stream()
                        .map(userMapper::toUserResponse)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toUserResponse)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void unlockUser(String id) {
        User user = userRepository.findById(Long.parseLong(id))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setIsLocked(false);
        user.setIsActive(true);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void lockUser(String id) {
        User user = userRepository.findById(Long.parseLong(id))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String currentUserEmail = AppUtil.emailFromAuthentication();
        if (user.getEmail().equals(currentUserEmail)) {
            throw new AppException(ErrorCode.CANNOT_LOCK_OWN_ACCOUNT);
        }

        user.setIsLocked(true);
        user.setIsActive(false);
    }
}
