package org.ngphthinh.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.ngphthinh.dto.response.ApiResponse;
import org.ngphthinh.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.builder()
                .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatusCode().value())
                .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                .timestamp(LocalDateTime.now())
                .error(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        ex.printStackTrace(); // Log the stack trace for debugging purposes

        return ResponseEntity
                .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatusCode()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        ));

        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                request.getRequestURI(),
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }


    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .status(ex.getErrorCode().getStatusCode().value())
                .message(HttpStatus.valueOf(ex.getErrorCode().getStatusCode().value()).getReasonPhrase())
                .timestamp(LocalDateTime.now())
                .error(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(ex.getErrorCode().getStatusCode()).body(response);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        {
            return ResponseEntity.status(ErrorCode.FORBIDDEN.getStatusCode())
                    .body(ErrorResponse.builder()
                            .timestamp(LocalDateTime.now())
                            .status(ErrorCode.FORBIDDEN.getStatusCode().value())
                            .error(HttpStatus.valueOf(ErrorCode.FORBIDDEN.getStatusCode().value()).getReasonPhrase())
                            .message(ErrorCode.FORBIDDEN.getMessage())
                            .path(request.getRequestURI())
                            .build());
        }
    }

}