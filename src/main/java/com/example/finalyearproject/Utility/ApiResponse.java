package com.example.finalyearproject.Utility;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String message;
    private T data;
    private List<String> errors;

    // Success response with data
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(message, data, null);
    }

    // Success response without data
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(message, null, null);
    }

    // Error response with message
    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        return new ApiResponse<>(message, null, errors);
    }

    // Error response with single error
    public static <T> ApiResponse<T> error(String message, String error) {
        return new ApiResponse<>(message, null, List.of(error));
    }
}

