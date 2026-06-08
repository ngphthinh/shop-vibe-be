package org.ngphthinh.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User mockUser;
    private UserResponse mockUserResponse;
    private final String EMAIL_USER = "user@shopvibe.vn";
    private final String EMAIL_ADMIN = "admin@shopvibe.vn";

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email(EMAIL_USER)
                .fullName("Nguyễn Văn A")
                .isLocked(false)
                .isActive(true)
                .build();

        mockUserResponse = UserResponse.builder()
                .id(1L)
                .email(EMAIL_USER)
                .fullName("Nguyễn Văn A")
                .build();
    }

    // ==========================================
    // TEST FOR getMyProfile()
    // ==========================================
    @Nested
    @DisplayName("Test getMyProfile method")
    class GetMyProfileTest {

        @Test
        @DisplayName("Should return UserResponse when user exists")
        void getMyProfile_Success() {
            try (MockedStatic<AppUtil> mockedAppUtil = Mockito.mockStatic(AppUtil.class)) {
                mockedAppUtil.when(AppUtil::emailFromAuthentication).thenReturn(EMAIL_USER);
                when(userRepository.findByEmail(EMAIL_USER)).thenReturn(Optional.of(mockUser));
                when(userMapper.toUserResponse(mockUser)).thenReturn(mockUserResponse);

                UserResponse result = userService.getMyProfile();

                assertNotNull(result);
                assertEquals(EMAIL_USER, result.getEmail());
                verify(userRepository, times(1)).findByEmail(EMAIL_USER);
            }
        }

        @Test
        @DisplayName("Should throw USER_NOT_FOUND when user does not exist")
        void getMyProfile_UserNotFound() {
            try (MockedStatic<AppUtil> mockedAppUtil = Mockito.mockStatic(AppUtil.class)) {
                mockedAppUtil.when(AppUtil::emailFromAuthentication).thenReturn(EMAIL_USER);
                when(userRepository.findByEmail(EMAIL_USER)).thenReturn(Optional.empty());

                AppException exception = assertThrows(AppException.class, () -> userService.getMyProfile());
                assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
            }
        }
    }

    // ==========================================
    // TEST FOR updateUserProfile()
    // ==========================================
    @Nested
    @DisplayName("Test updateUserProfile method")
    class UpdateUserProfileTest {

        @Test
        @DisplayName("Should update and return UserResponse successfully")
        void updateUserProfile_Success() {
            UserUpdateRequest request = new UserUpdateRequest(); // Giả định DTO của bạn có các trường update
            
            try (MockedStatic<AppUtil> mockedAppUtil = Mockito.mockStatic(AppUtil.class)) {
                mockedAppUtil.when(AppUtil::emailFromAuthentication).thenReturn(EMAIL_USER);
                when(userRepository.findByEmail(EMAIL_USER)).thenReturn(Optional.of(mockUser));
                doNothing().when(userMapper).updateUserFromUserResponse(request, mockUser);
                when(userMapper.toUserResponse(mockUser)).thenReturn(mockUserResponse);

                UserResponse result = userService.updateUserProfile(request);

                assertNotNull(result);
                verify(userMapper, times(1)).updateUserFromUserResponse(request, mockUser);
            }
        }
    }

    // ==========================================
    // TEST FOR getAllUsers()
    // ==========================================
    @Nested
    @DisplayName("Test getAllUsers method")
    class GetAllUsersTest {

        @Test
        @DisplayName("Should return PagingResponse of UserResponse")
        void getAllUsers_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(mockUser), pageable, 1);

            when(userRepository.findAll(pageable)).thenReturn(userPage);
            when(userMapper.toUserResponse(mockUser)).thenReturn(mockUserResponse);

            PagingResponse<UserResponse> result = userService.getAllUsers(pageable);

            assertNotNull(result);
            assertEquals(0, result.getPage());
            assertEquals(1, result.getTotalElements());
            assertEquals(1, result.getContent().size());
        }
    }

    // ==========================================
    // TEST FOR getUserById()
    // ==========================================
    @Nested
    @DisplayName("Test getUserById method")
    class GetUserByIdTest {

        @Test
        @DisplayName("Should return UserResponse when ID exists")
        void getUserById_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
            when(userMapper.toUserResponse(mockUser)).thenReturn(mockUserResponse);

            UserResponse result = userService.getUserById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("Should throw USER_NOT_FOUND when ID does not exist")
        void getUserById_NotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            AppException exception = assertThrows(AppException.class, () -> userService.getUserById(99L));
            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        }
    }

    // ==========================================
    // TEST FOR unlockUser()
    // ==========================================
    @Nested
    @DisplayName("Test unlockUser method")
    class UnlockUserTest {

        @Test
        @DisplayName("Should unlock user successfully when ID exists")
        void unlockUser_Success() {
            mockUser.setIsLocked(true);
            mockUser.setIsActive(false);

            when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

            userService.unlockUser("1");

            assertFalse(mockUser.getIsLocked());
            assertTrue(mockUser.getIsActive());
        }
    }

    // ==========================================
    // TEST FOR lockUser()
    // ==========================================
    @Nested
    @DisplayName("Test lockUser method")
    class LockUserTest {

        @Test
        @DisplayName("Should lock user successfully when admin locks another user")
        void lockUser_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

            try (MockedStatic<AppUtil> mockedAppUtil = Mockito.mockStatic(AppUtil.class)) {
                mockedAppUtil.when(AppUtil::emailFromAuthentication).thenReturn(EMAIL_ADMIN);

                userService.lockUser("1");

                assertTrue(mockUser.getIsLocked());
                assertFalse(mockUser.getIsActive());
            }
        }

        @Test
        @DisplayName("Should throw CANNOT_LOCK_OWN_ACCOUNT when admin locks themselves")
        void lockUser_CannotLockOwnAccount() {
            // Giả lập user cần khóa chính là Admin đang đăng nhập
            mockUser.setEmail(EMAIL_ADMIN); 
            when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

            try (MockedStatic<AppUtil> mockedAppUtil = Mockito.mockStatic(AppUtil.class)) {
                mockedAppUtil.when(AppUtil::emailFromAuthentication).thenReturn(EMAIL_ADMIN);

                AppException exception = assertThrows(AppException.class, () -> userService.lockUser("1"));
                assertEquals(ErrorCode.CANNOT_LOCK_OWN_ACCOUNT, exception.getErrorCode());
            }
        }
    }
}