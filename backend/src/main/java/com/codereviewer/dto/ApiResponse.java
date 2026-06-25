package com.codereviewer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String error;
    private LocalDateTime timestamp;

    private ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.message = message;
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> error(String errorMsg) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = false;
        r.error = errorMsg;
        return r;
    }

    // For GlobalExceptionHandler builder-style usage
    public static <T> Builder<T> builder() { return new Builder<>(); }

    public static class Builder<T> {
        private final ApiResponse<T> obj = new ApiResponse<>();

        public Builder<T> success(boolean b)    { obj.success = b; return this; }
        public Builder<T> message(String s)     { obj.message = s; return this; }
        public Builder<T> data(T d)             { obj.data = d; return this; }
        public Builder<T> error(String s)       { obj.error = s; return this; }
        public ApiResponse<T> build()           { return obj; }
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public String getError() { return error; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
