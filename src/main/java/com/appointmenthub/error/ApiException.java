package com.appointmenthub.error;

/**
 * Exception raised when a request cannot be completed according to the business
 * rules. It carries the functional {@link ErrorCode} used to build the HTTP
 * response, so the API layer never has to guess a status code.
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public int httpStatus() {
        return errorCode.httpStatus();
    }

    public static ApiException validation(String message) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, message);
    }

    public static ApiException notFound(ErrorCode errorCode, String message) {
        return new ApiException(errorCode, message);
    }

    public static ApiException conflict(ErrorCode errorCode, String message) {
        return new ApiException(errorCode, message);
    }
}
