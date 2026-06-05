package org.ngphthinh.service;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.SignedJWT;
import com.sun.jdi.request.DuplicateRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ngphthinh.dto.request.user.*;
import org.ngphthinh.dto.response.user.AuthenticateResponse;
import org.ngphthinh.dto.response.user.IntrospectResponse;
import org.ngphthinh.dto.response.user.UserChangePasswordResponse;
import org.ngphthinh.dto.response.user.UserResponse;
import org.ngphthinh.entity.Cart;
import org.ngphthinh.entity.Role;
import org.ngphthinh.entity.User;
import org.ngphthinh.enums.RoleName;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;
import org.ngphthinh.exception.auth.AccountLockedException;
import org.ngphthinh.exception.auth.BadCredentialsException;
import org.ngphthinh.exception.auth.DuplicateEmailException;
import org.ngphthinh.exception.auth.TokenExpiredException;
import org.ngphthinh.mapper.UserMapper;
import org.ngphthinh.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final ChangePasswordService changePasswordService;
    private final TokenBlackListService tokenBlackListService;
    private final UserClockerService userClockerService;
    private final UserService userService;

    public UserResponse register(@Valid UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("email", request.getEmail());
        }

        User user = userMapper.toUser(request);

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        user.setCart(Cart.builder()
                .user(user)
                .build());

        user.setRoles(Set.of(Role.builder()
                .id(RoleName.ROLE_USER.name())
                .name(RoleName.ROLE_USER)
                .build()));

        return userMapper.toUserResponse(userRepository.save(user));

    }

    @Transactional
    public AuthenticateResponse authenticate(AuthenticateRequest request) {
        User user = userRepository.findByEmail((request.getEmail()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getIsLocked()) {
            throw new AccountLockedException();
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {

            int failedAttempts = userClockerService.incrementFailedAttempts(request.getEmail());

            if (failedAttempts >= 5) {
                log.warn("User {} has been locked due to too many failed login attempts", request.getEmail());

                userService.lockUser(request.getEmail());
                userClockerService.resetFailedAttempts(request.getEmail());
                throw new AccountLockedException();
            }

            throw new BadCredentialsException();
        }

        String token = jwtService.generateAccessToken(user);

        return AuthenticateResponse.builder()
                .accessToken(token)
                .user(userMapper.toUserResponse(user))
                .expiresIn(jwtService.getExpiration())
                .refreshToken(jwtService.generateRefreshToken(user))
                .build();
    }


    public IntrospectResponse introspect(IntrospectRequest request) {
        return jwtService.introspect(request.getAccessToken());
    }

    public AuthenticateResponse refreshToken(RefreshTokenRequest refreshToken) {

        String refreshTokenStr = refreshToken.getRefreshToken();

        if (!refreshTokenService.isRefreshTokenExists(refreshTokenStr)) {
            throw new TokenExpiredException();
        }

        String email = refreshTokenService.getUser(refreshTokenStr);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        refreshTokenService.deleteRefreshToken(refreshTokenStr);
        refreshTokenService.saveRefreshToken(newRefreshToken, email);

        return AuthenticateResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(userMapper.toUserResponse(user))
                .expiresIn(jwtService.getExpiration())
                .build();

    }


    @PreAuthorize("#request.email == authentication.name")
    public UserChangePasswordResponse changePassword(UserChangePasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException();
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        long currentTime = new Date().toInstant().getEpochSecond();
        changePasswordService.saveTokenInvalidationTimestamp(request.getEmail(), String.valueOf(currentTime));

        return UserChangePasswordResponse.builder()
                .success(true)
                .build();
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    public void logout(LogoutRequest request) {
        String token = request.getAccessToken();
        String refreshToken = request.getRefreshToken();
        SignedJWT signedJWT = jwtService.verifyToken(token);

        try {
            String jitToken = signedJWT.getJWTClaimsSet().getJWTID();
            long expirationTime = getSecondsUntilExpiration(signedJWT.getJWTClaimsSet().getExpirationTime());

            // store blacklist with time to live equals to the remaining time of the token
            tokenBlackListService.blacklistToken(jitToken, expirationTime);

            refreshTokenService.deleteRefreshToken(refreshToken);
        } catch (ParseException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

    }

    private long getSecondsUntilExpiration(Date expirationDate) {
        long expirationEpoch = expirationDate.toInstant().getEpochSecond();
        long nowEpoch = Instant.now().getEpochSecond();
        return expirationEpoch - nowEpoch;
    }
}
