package com.mostafa.eticket.exception;

import com.mostafa.eticket.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex) {

    Map<String, String> validationErrors = new HashMap<>();

    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> validationErrors.put(error.getField(), error.getDefaultMessage()));

    ErrorResponse response =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            validationErrors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableMessage(
      HttpMessageNotReadableException ex) {
    ErrorResponse response =
        new ErrorResponse(
            LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "Malformed request body", null);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    ErrorResponse response =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'",
            null);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(TicketNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleTicketNotFoundException(TicketNotFoundException ex) {
    ErrorResponse response =
        new ErrorResponse(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), ex.getMessage(), null);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  @ExceptionHandler(InvalidStatusTransitionException.class)
  public ResponseEntity<ErrorResponse> handleInvalidStatusTransitionException(
      InvalidStatusTransitionException ex) {
    ErrorResponse response =
        new ErrorResponse(
            LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), ex.getMessage(), null);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(InvalidPageSizeException.class)
  public ResponseEntity<ErrorResponse> handleInvalidPageSizeException(InvalidPageSizeException ex) {
    ErrorResponse response =
        new ErrorResponse(
            LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), ex.getMessage(), null);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
    ErrorResponse response =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.UNAUTHORIZED.value(),
            "Invalid username or password",
            null);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }

  @ExceptionHandler(DuplicateUserException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateUserException(DuplicateUserException ex) {
    ErrorResponse response =
        new ErrorResponse(LocalDateTime.now(), HttpStatus.CONFLICT.value(), ex.getMessage(), null);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
  }

  @ExceptionHandler(InvalidInvitationException.class)
  public ResponseEntity<ErrorResponse> handleInvalidInvitationException(
      InvalidInvitationException ex) {
    ErrorResponse response =
        new ErrorResponse(
            LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), ex.getMessage(), null);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    ErrorResponse response =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal server error",
            null);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}
