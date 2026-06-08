package org.ngphthinh.exception;

import com.sun.jdi.request.DuplicateRequestException;
import jakarta.servlet.http.HttpServletRequest;
import org.ngphthinh.dto.response.ApiResponse;
import org.ngphthinh.dto.response.ErrorResponse;
import org.ngphthinh.exception.auth.DuplicateEmailException;
import org.ngphthinh.exception.order.InvalidStatusTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

    private String mapAttribute(String message, String attributeKey, String attributeValue) {
        if (message.contains("{" + attributeKey + "}")) {
            return message.replace("{" + attributeKey + "}", attributeValue);
        }
        return message;
    }

    private String mapAttributes(String message, Map<String, String> attributes) {
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            message = mapAttribute(message, entry.getKey(), entry.getValue());
        }
        return message;
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmailException(DuplicateEmailException ex, HttpServletRequest request) {


        String message = ex.getErrorCode().getMessage();

        if (Objects.nonNull(ex.getKeyAttribute()) && Objects.nonNull(ex.getAttributeValue())) {
            message = mapAttribute(message, ex.getKeyAttribute(), ex.getAttributeValue());
        }
        ErrorResponse response = ErrorResponse.builder()
                .status(ex.getErrorCode().getStatusCode().value())
                .message(HttpStatus.valueOf(ex.getErrorCode().getStatusCode().value()).getReasonPhrase())
                .timestamp(LocalDateTime.now())
                .error(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(ex.getErrorCode().getStatusCode()).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        {
            ErrorCode errorCode = ErrorCode.INVALID_PARAMETER_TYPE;
            String message = e.getMessage();


            Class<?> errorTypeClass = e.getRequiredType();

            Set<Class<?>> NUMBER_TYPES = Set.of(
                    Byte.class, Short.class, Integer.class, Long.class,
                    Float.class, Double.class,
                    byte.class, short.class, int.class, long.class,
                    float.class, double.class
            );
            if (errorTypeClass == null) {
                errorTypeClass = e.getParameter().getParameterType();
            }

            if (errorTypeClass == LocalDate.class) {
                errorCode = ErrorCode.INVALID_DATE_FORMAT;
            } else if (NUMBER_TYPES.contains(errorTypeClass)) {
                errorCode = ErrorCode.INVALID_NUMBER_FORMAT;
            }

            if (e.getValue() != null) {
                message = String.format("Invalid value '%s' for parameter '%s' must be a %s", e.getValue(), e.getName(), errorTypeClass.getSimpleName());
            }

            ErrorResponse response = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(errorCode.getStatusCode().value())
                    .error(HttpStatus.valueOf(errorCode.getStatusCode().value()).getReasonPhrase())
                    .message(message)
                    .path(request.getRequestURI())
                    .build();

            return ResponseEntity.status(errorCode.getStatusCode()).body(response);

        }
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleObjectOptimisticLockingFailureException(ObjectOptimisticLockingFailureException e, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ErrorCode.OPTIMISTIC_LOCKING_FAILURE.getStatusCode().value())
                .error(HttpStatus.valueOf(ErrorCode.OPTIMISTIC_LOCKING_FAILURE.getStatusCode().value()).getReasonPhrase())
                .message(ErrorCode.OPTIMISTIC_LOCKING_FAILURE.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(ErrorCode.OPTIMISTIC_LOCKING_FAILURE.getStatusCode()).body(response);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransitionException(InvalidStatusTransitionException e, HttpServletRequest request) {

        String message = e.getErrorCode().getMessage();

        if (Objects.nonNull(e.getKeyNewStatus()) && Objects.nonNull(e.getAttributeNewStatus()) && Objects.nonNull(e.getKeyOldStatus()) && Objects.nonNull(e.getAttributeOldStatus())) {
            Map<String, String> attributes = Map.of(
                    e.getKeyNewStatus(), e.getAttributeNewStatus(),
                    e.getKeyOldStatus(), e.getAttributeOldStatus()
            );
            message = mapAttributes(message, attributes);
        }

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(e.getErrorCode().getStatusCode().value())
                .error(HttpStatus.valueOf(e.getErrorCode().getStatusCode().value()).getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(e.getErrorCode().getStatusCode()).body(response);
    }
}