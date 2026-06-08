package org.ngphthinh.util;

import jakarta.servlet.http.HttpServletRequest;
import org.ngphthinh.dto.response.ErrorResponse;
import org.ngphthinh.exception.AppException;
import org.ngphthinh.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;


public class AppUtil {
    public static ErrorResponse generateErrorResponse(HttpServletRequest request, ErrorCode errorCode) {
        return ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(errorCode.getStatusCode().value())
                .error(HttpStatus.valueOf(errorCode.getStatusCode().value()).getReasonPhrase())
                .message(errorCode.getMessage())
                .path(request.getRequestURI())
                .build();
    }

    public static Pageable buildPageable(int page, int size, String sort) {
        int validatedSize = Math.min(size, 50);
        Sort sortBy = switch (sort) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.by("createdAt").descending();
        };

        return PageRequest.of(page, validatedSize, sortBy);
    }


    /**
     * Lấy email của người dùng đã xác thực từ SecurityContextHolder.
     * @return Email của người dùng đã xác thực.
     * @throws AppException nếu không có người dùng nào đã xác thực hoặc người dùng không có quyền truy cập.
     */
    public static String emailFromAuthentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return authentication.getName();
    }
}
