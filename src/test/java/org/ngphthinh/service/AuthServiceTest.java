package org.ngphthinh.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ngphthinh.dto.request.user.AuthenticateRequest;
import org.ngphthinh.dto.request.user.RefreshTokenRequest;
import org.ngphthinh.dto.request.user.UserCreateRequest;
import org.ngphthinh.dto.response.user.AuthenticateResponse;
import org.ngphthinh.dto.response.user.UserResponse;
import org.ngphthinh.entity.User;
import org.ngphthinh.exception.auth.AccountLockedException;
import org.ngphthinh.exception.auth.BadCredentialsException;
import org.ngphthinh.exception.auth.DuplicateEmailException;
import org.ngphthinh.exception.auth.TokenExpiredException;
import org.ngphthinh.mapper.UserMapper;
import org.ngphthinh.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authService;

    private UserCreateRequest request;
    private User user;
    private UserResponse userResponse;

    private AuthenticateRequest authenticateRequest;
    private AuthenticateResponse authenticateResponse;

    @Mock
    private UserService userService;

    @Mock
    private UserClockerService userClockerService;

    private RefreshTokenRequest refreshTokenRequest;

    @BeforeEach
    void setUp() {
        request = UserCreateRequest.builder()
                .email("test@gmail.com")
                .password("password")
                .fullName("Test User")
                .phone("1234567890")
                .build();

        user = User.builder()
                .email("test@gmail.com")
                .password("encoded")
                .fullName("Test User")
                .phone("1234567890")
                .isActive(true)
                .isLocked(false)
                .build();

        userResponse = UserResponse.builder()
                .email("test@gmail.com")
                .fullName("Test User")
                .roles(List.of("ROLE_USER"))
                .build();

        authenticateRequest = AuthenticateRequest.builder()
                .email("test@gmail.com")
                .password("password")
                .build();

        authenticateResponse = AuthenticateResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .user(userResponse)
                .expiresIn(3600L)
                .build();

        refreshTokenRequest = RefreshTokenRequest.builder()
                .refreshToken("refresh-token")
                .build();
    }

    @Test
    void register_success() {


        when(userRepository.existsByEmail("test@gmail.com"))
                .thenReturn(false);

        when(userMapper.toUser(any(UserCreateRequest.class)))
                .thenReturn(user);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded");

        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        when(userMapper.toUserResponse(any(User.class)))
                .thenReturn(userResponse);

        UserResponse response = authService.register(request);

        assertEquals("test@gmail.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
    }

    @Test
    void register_duplicateEmail() {
        when(userRepository.existsByEmail("test@gmail.com"))
                .thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void authenticate_success() {
        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password", "encoded"))
                .thenReturn(true);

        when(jwtService.generateAccessToken(user))
                .thenReturn("access-token");

        when(jwtService.generateRefreshToken(user))
                .thenReturn("refresh-token");
        when(userMapper.toUserResponse(any(User.class)))
                .thenReturn(userResponse);
        AuthenticateResponse response = authService.authenticate(authenticateRequest);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("test@gmail.com", response.getUser().getEmail());
    }

    @Test
    void login_wrongPassword() {
        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));


        when(passwordEncoder.matches("password", "encoded"))
                .thenReturn(false);

        when(userClockerService.incrementFailedAttempts(anyString()))
                .thenReturn(1);

        assertThrows(BadCredentialsException.class, () -> authService.authenticate(authenticateRequest));
    }

    @Test
    void login_lockedAccount() {

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        user.setIsLocked(true);

        assertThrows(AccountLockedException.class, () -> authService.authenticate(authenticateRequest));

        verify(jwtService, never()).generateAccessToken(any());
    }
    @Test
    void login_lockAccountAfter5FailedAttempts() {
        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password", "encoded"))
                .thenReturn(false);

        when(userClockerService.incrementFailedAttempts(anyString()))
                .thenReturn(5);

        doNothing().when(userService).lockUser(anyString());
        assertThrows(AccountLockedException.class, () -> authService.authenticate(authenticateRequest));

        verify(userClockerService).resetFailedAttempts(anyString());
    }

    @Test
    void refreshToken_success() {

        when(refreshTokenService.isRefreshTokenExists(refreshTokenRequest.getRefreshToken()))
                .thenReturn(true);


        when(refreshTokenService.getUser(refreshTokenRequest.getRefreshToken()))
                .thenReturn(user.getEmail());
        when(jwtService.generateAccessToken(user))
                .thenReturn("new-access-token");

        when(jwtService.generateRefreshToken(user))
                .thenReturn("new-refresh-token");

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        AuthenticateResponse response = authService.refreshToken(refreshTokenRequest);

        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
    }

    @Test
    void refreshToken_invalidRefreshToken() {
        when(refreshTokenService.isRefreshTokenExists(refreshTokenRequest.getRefreshToken()))
                .thenReturn(false);

        assertThrows(TokenExpiredException.class, () -> authService.refreshToken(refreshTokenRequest));
    }
}

