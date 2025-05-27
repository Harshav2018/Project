package com.example.finalyearproject.Utility;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class ConsumerRegisterDTO {

    @NotBlank(message = "First Name cannot be blank")
    private String consumerFirstName;

    @NotBlank(message = "Last Name cannot be blank")
    private String consumerLastName;

    @NotBlank(message = "Password cannot be blank")
    private String consumerPassword;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String consumerEmail;

    @NotBlank(message = "Phone number cannot be blank")
    @Pattern(regexp = "^(\\+91|0)?\\d{10}$", message = "Invalid phone number")
    private String consumerPhone;

    @NotBlank(message = "Address cannot be blank")
    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String consumerAddress;

    private MultipartFile profilePhoto;
}