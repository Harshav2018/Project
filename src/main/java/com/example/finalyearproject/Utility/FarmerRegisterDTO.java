package com.example.finalyearproject.Utility;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class FarmerRegisterDTO {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String farmerEmail;

    @NotBlank(message = "First name cannot be blank")
    private String firstName;

    @NotBlank(message = "Last name cannot be blank")
    private String lastName;

    @NotBlank(message = "Password cannot be blank")
    private String farmerPassword;

    @NotBlank(message = "Phone number cannot be blank")
    @Pattern(regexp = "^(\\+91|0)?\\d{10}$", message = "Invalid phone number")
    private String farmerPhone;

    @NotBlank(message = "Address cannot be blank")
    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String farmerAddress;

    private MultipartFile profilePhoto;
}
