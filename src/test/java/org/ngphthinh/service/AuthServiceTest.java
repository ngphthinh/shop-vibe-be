package org.ngphthinh.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ngphthinh.dto.request.user.*;
import org.ngphthinh.dto.response.user.AuthenticateResponse;
import org.ngphthinh.dto.response.user.IntrospectResponse;
import org.ngphthinh.dto.response.user.UserChangePasswordResponse;
import org.ngphthinh.dto.response.user.UserResponse;
import org.ngphthinh.entity.User;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;
import org.ngphthinh.exception.auth.AccountLockedException;
import org.ngphthinh.exception.auth.BadCredentialsException;
import org.ngphthinh.exception.auth.DuplicateEmailException;
import org.ngphthinh.exception.auth.TokenExpiredException;
import org.ngphthinh.mapper.UserMapper;
import org.ngphthinh.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ChangePasswordService changePasswordService;

    @Mock
    private TokenBlackListService tokenBlackListService;

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
        // 1. SETUP (Chuẩn bị các Mock hành vi có trả về giá trị)
        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password", "encoded"))
                .thenReturn(false);

        when(userClockerService.incrementFailedAttempts(anyString()))
                .thenReturn(5);

        // Đối với các hàm void như lockAccountDueToMaxFailedAttempts và resetFailedAttempts,
        // Mockito tự động "do nothing" nên bạn KHÔNG CẦN viết doNothing().when(...) nữa.
        // Việc bỏ doNothing() sẽ giải quyết TRIỆT ĐỂ lỗi UnnecessaryStubbingException.

        // 2. ACT & ASSERT (Kỳ vọng ném ra ngoại lệ AccountLockedException)
        assertThrows(AccountLockedException.class, () -> authService.authenticate(authenticateRequest));

        // 3. VERIFY (Kiểm tra xem các hàm void có được gọi đúng thứ tự và chính xác không)
        // Kiểm tra hàm khóa tài khoản được gọi đúng 1 lần
        verify(userService, times(1)).lockAccountDueToMaxFailedAttempts("test@gmail.com");

        // Kiểm tra hàm reset số lần fail ĐƯỢC GỌI đúng 1 lần (khớp với logic code thực tế của bạn)
        verify(userClockerService, times(1)).resetFailedAttempts("test@gmail.com");
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
    @Test
    void changePassword_shouldSuccess_whenDataIsValid() {
        // 1. SETUP
        UserChangePasswordRequest request = new UserChangePasswordRequest();
        request.setEmail("test@gmail.com");
        request.setOldPassword("oldPass");
        request.setNewPassword("newPass");

        User user = new User();
        user.setEmail("test@gmail.com");
        user.setPassword("encodedOldPass");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "encodedOldPass")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPass");
        when(userRepository.save(user)).thenReturn(user);

        // Mock hàm void lưu timestamp invalided token
        doNothing().when(changePasswordService).saveTokenInvalidationTimestamp(eq("test@gmail.com"), anyString());

        // 2. ACT
        UserChangePasswordResponse response = authService.changePassword(request);

        // 3. ASSERT & VERIFY
        assertNotNull(response);
        assertTrue(response.isSuccess());

        verify(userRepository).save(user);
        verify(changePasswordService).saveTokenInvalidationTimestamp(eq("test@gmail.com"), anyString());
    }

    @Test
    void changePassword_shouldThrowBadCredentialsException_whenOldPasswordDoesNotMatch() {
        // 1. SETUP
        UserChangePasswordRequest request = new UserChangePasswordRequest();
        request.setEmail("test@gmail.com");
        request.setOldPassword("wrongOldPass");

        User user = new User();
        user.setPassword("encodedOldPass");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        // Ép trả về false khi khớp mật khẩu cũ
        when(passwordEncoder.matches("wrongOldPass", "encodedOldPass")).thenReturn(false);

        // 2. ACT & ASSERT
        assertThrows(BadCredentialsException.class, () -> authService.changePassword(request));

        // 3. VERIFY (Mật khẩu sai thì tuyệt đối không lưu hay thay đổi gì cả)
        verify(userRepository, never()).save(any(User.class));
        verify(changePasswordService, never()).saveTokenInvalidationTimestamp(anyString(), anyString());
    }

    // ==========================================
    // TẬP TEST CASES CHO: logout
    // ==========================================

    @Test
    void logout_shouldSuccess_whenTokensAreValid() throws ParseException {
        // 1. SETUP
        LogoutRequest request = new LogoutRequest();
        request.setAccessToken("valid.access.token");
        request.setRefreshToken("valid_refresh_token");

        // Mock thư viện SignedJWT và JWTClaimsSet của Nimbus
        SignedJWT signedJWTMock = mock(SignedJWT.class);
        JWTClaimsSet claimsSetMock = mock(JWTClaimsSet.class);

        // Giả lập token hết hạn sau 1 tiếng (3600 giây) tính từ bây giờ
        Date expirationDate = Date.from(Instant.now().plusSeconds(3600));

        when(jwtService.verifyToken("valid.access.token")).thenReturn(signedJWTMock);
        when(signedJWTMock.getJWTClaimsSet()).thenReturn(claimsSetMock);
        when(claimsSetMock.getJWTID()).thenReturn("jit-uuid-12345");
        when(claimsSetMock.getExpirationTime()).thenReturn(expirationDate);

        // 2. ACT
        assertDoesNotThrow(() -> authService.logout(request));

        // 3. VERIFY (Kiểm tra xem đã lưu vào blacklist và xóa refresh token chưa)
        verify(tokenBlackListService, times(1)).blacklistToken(eq("jit-uuid-12345"), anyLong());
        verify(refreshTokenService, times(1)).deleteRefreshToken("valid_refresh_token");
    }

    @Test
    void logout_shouldThrowAppException_whenParseExceptionOccurs() throws ParseException {
        // 1. SETUP
        LogoutRequest request = new LogoutRequest();
        request.setAccessToken("invalid.access.token");

        SignedJWT signedJWTMock = mock(SignedJWT.class);
        when(jwtService.verifyToken("invalid.access.token")).thenReturn(signedJWTMock);

        // Ép ném ra ParseException tại dòng lấy ClaimsSet để bao phủ khối catch(ParseException)
        when(signedJWTMock.getJWTClaimsSet()).thenThrow(new ParseException("Invalid structure", 0));

        // 2. ACT & ASSERT

        AppException exception = assertThrows(AppException.class, () -> authService.logout(request));

        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());

        // 3. VERIFY (Lỗi cấu trúc token thì không thực hiện đưa vào blacklist hay xóa DB)
        verify(tokenBlackListService, never()).blacklistToken(anyString(), anyLong());
        verify(refreshTokenService, never()).deleteRefreshToken(anyString());
    }

    @Test
    void introspect_shouldReturnResponse_whenTokenIsValidAndAccountNotLocked() {
        // 1. SETUP (Chuẩn bị dữ liệu: Token đúng + Tài khoản không bị khóa)
        IntrospectRequest request = new IntrospectRequest();
        request.setAccessToken("valid_jwt_token");

        IntrospectResponse mockResponse = IntrospectResponse.builder()
                .active(true)
                .sub("user@gmail.com") // sub thường chứa email/username của chủ token
                .build();

        when(jwtService.introspect("valid_jwt_token")).thenReturn(mockResponse);
        // Ép trả về false (tài khoản KHÔNG bị khóa)
        when(userRepository.existsByEmailAndIsLockedTrue("user@gmail.com")).thenReturn(false);

        // 2. ACT (Chạy hàm thực tế)
        IntrospectResponse actualResponse = authService.introspect(request);

        // 3. ASSERT (Kiểm tra kết quả trả về)
        assertNotNull(actualResponse);
        assertTrue(actualResponse.isActive());
        assertEquals("user@gmail.com", actualResponse.getSub());

        // 4. VERIFY (Xác nhận các tầng phụ thuộc được gọi đúng luồng)
        verify(jwtService, times(1)).introspect("valid_jwt_token");
        verify(userRepository, times(1)).existsByEmailAndIsLockedTrue("user@gmail.com");
    }

    @Test
    void introspect_shouldThrowAccountLockedException_whenAccountIsLocked() {
        // 1. SETUP (Chuẩn bị dữ liệu: Token đúng nhưng Tài khoản ĐÃ BỊ KHÓA)
        IntrospectRequest request = new IntrospectRequest();
        request.setAccessToken("valid_jwt_token_of_locked_user");

        IntrospectResponse mockResponse = IntrospectResponse.builder()
                .active(true)
                .sub("locked_user@gmail.com")
                .build();

        when(jwtService.introspect("valid_jwt_token_of_locked_user")).thenReturn(mockResponse);
        // Ép trả về true (Tài khoản ĐÃ bị khóa hệ thống)
        when(userRepository.existsByEmailAndIsLockedTrue("locked_user@gmail.com")).thenReturn(true);

        // 2. ACT & ASSERT (Kích hoạt hàm và kiểm tra xem có ném đúng Exception mong muốn)
        assertThrows(AccountLockedException.class, () ->
                authService.introspect(request)
        );

        // 3. VERIFY
        verify(jwtService, times(1)).introspect("valid_jwt_token_of_locked_user");
        verify(userRepository, times(1)).existsByEmailAndIsLockedTrue("locked_user@gmail.com");
    }

    @Test
    void refreshToken_shouldThrowAccountLockedException_whenUserIsLocked() {
        // 1. SETUP (Chuẩn bị dữ liệu mẫu)
        String mockRefreshTokenStr = "valid_refresh_token_but_user_locked";
        String userEmail = "locked_user@gmail.com";

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(mockRefreshTokenStr);

        // Giả lập đối tượng User đang bị khóa
        User lockedUser = new User();
        lockedUser.setEmail(userEmail);
        lockedUser.setIsLocked(true); // Ngòi nổ để kích hoạt khối if

        // Giả lập hành vi của các Service phụ thuộc
        when(refreshTokenService.isRefreshTokenExists(mockRefreshTokenStr)).thenReturn(true);
        when(refreshTokenService.getUser(mockRefreshTokenStr)).thenReturn(userEmail);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(lockedUser));

        // 2. ACT & ASSERT (Chạy hàm và kiểm tra ngoại lệ ném ra)
        assertThrows(AccountLockedException.class, () ->
                authService.refreshToken(request)
        );

        // 3. VERIFY (Bảo đảm tiến trình ngắt ngay lập tức, KHÔNG sinh token mới hay lưu DB)
        verify(refreshTokenService, times(1)).isRefreshTokenExists(mockRefreshTokenStr);
        verify(refreshTokenService, times(1)).getUser(mockRefreshTokenStr);
        verify(userRepository, times(1)).findByEmail(userEmail);

        // Khẳng định chắc chắn hệ thống không chạy xuống các dòng logic bên dưới
        verify(jwtService, never()).generateAccessToken(any(User.class));
        verify(jwtService, never()).generateRefreshToken(any(User.class));
        verify(refreshTokenService, never()).deleteRefreshToken(anyString());
        verify(refreshTokenService, never()).saveRefreshToken(anyString(), anyString());
    }
}

