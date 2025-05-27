package com.example.finalyearproject.Utility;

import com.example.finalyearproject.DataStore.Consumer;
import lombok.*;

@Data
public class ConsumerUtility {
    private int statusCode;
    private String message;
    private Consumer data;

    // Your existing constructors...

    // Add a method to convert to ApiResponse
    public ApiResponse<Consumer> toApiResponse() {
        if (statusCode >= 200 && statusCode < 300) {
            return ApiResponse.success(message, data);
        } else {
            return ApiResponse.error(message, "Failed consumer operation");
        }
    }
}