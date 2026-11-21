package ma.olivesmart.contract;

import java.time.Instant;
import java.util.Map;

public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final Instant timestamp;
    private final ApiError error;

    private ApiResponse(
            boolean success,
            String message,
            T data,
            ApiError error
    ) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now();
        this.error = error;
    }

    public static <T> ApiResponse<T> success(
            String message,
            T data
    ) {
        return new ApiResponse<>(
                true,
                message,
                data,
                null
        );
    }


    public static <T> ApiResponse<T> failure(
            String message,
            String errorCode
    ) {
        return new ApiResponse<>(
                false,
                message,
                null,
                new ApiError(errorCode, message)
        );
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public ApiError getError() {
        return error;
    }
}
