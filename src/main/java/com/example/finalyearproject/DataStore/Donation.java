package com.example.finalyearproject.DataStore;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Donation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int DonationId;

    @NotNull(message = "Donation amount cannot be null")
    @Positive(message = "Donation amount must be positive")
    private double Amount;


    private LocalDateTime DonationDate;

    @NotBlank(message = "Payment method cannot be blank")
    @Size(max = 50, message = "Payment method cannot exceed 50 characters")
    private String PaymentMethod;

    @ManyToOne()
    @JsonBackReference("consumer-donations")
    private Consumer consumer;

    @ManyToOne()
    @JsonBackReference("farmer-donations")
    private Farmer farmer;

}
