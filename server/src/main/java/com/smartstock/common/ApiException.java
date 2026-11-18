package com.smartstock.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class ApiException extends ErrorResponseException {

	private ApiException(HttpStatus status, String detail) {
		super(status, ProblemDetail.forStatusAndDetail(status, detail), null);
	}

	public static ApiException notFound(String what) {
		return new ApiException(HttpStatus.NOT_FOUND, what + " not found");
	}

	public static ApiException conflict(String detail) {
		return new ApiException(HttpStatus.CONFLICT, detail);
	}

	/** A well-formed request that breaks a business rule, such as selling more than the stock. */
	public static ApiException rejected(String detail) {
		return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, detail);
	}

	public static ApiException unauthorized(String detail) {
		return new ApiException(HttpStatus.UNAUTHORIZED, detail);
	}

	public static ApiException forbidden(String detail) {
		return new ApiException(HttpStatus.FORBIDDEN, detail);
	}

	public static ApiException badRequest(String detail) {
		return new ApiException(HttpStatus.BAD_REQUEST, detail);
	}

}
