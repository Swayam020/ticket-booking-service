package com.swayam.booking.api;

import com.swayam.booking.NotFoundException;
import com.swayam.booking.reservation.SeatAlreadyBookedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Collectors;

@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<Dtos.ApiError> handleNotFound(NotFoundException e) {
		return error(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(SeatAlreadyBookedException.class)
	ResponseEntity<Dtos.ApiError> handleSeatTaken(SeatAlreadyBookedException e) {
		return error(HttpStatus.CONFLICT, e.getMessage());
	}

	// every bad field in one response, sorted so the message is stable for clients and tests
	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<Dtos.ApiError> handleInvalid(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.sorted(Comparator.comparing(FieldError::getField))
				.map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return error(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<Dtos.ApiError> handleUnreadable(HttpMessageNotReadableException e) {
		return error(HttpStatus.BAD_REQUEST, "malformed request body");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<Dtos.ApiError> handleBadId(MethodArgumentTypeMismatchException e) {
		return error(HttpStatus.BAD_REQUEST, "invalid id");
	}

	private static ResponseEntity<Dtos.ApiError> error(HttpStatus status, String message) {
		return ResponseEntity.status(status)
				.body(new Dtos.ApiError(status.getReasonPhrase(), message, Instant.now()));
	}

}
