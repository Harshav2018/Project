package com.example.finalyearproject.DataStore;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryAddresses {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_address_id", nullable = false)
    private int deliveryAddressId;

    @NotBlank(message = "Street address cannot be blank")
    @Size(max = 255, message = "Street address cannot exceed 255 characters")
    private String streetAddress;

    @NotBlank(message = "City cannot be blank")
    private String city;

    @NotBlank(message = "Pincode cannot be empty")
    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid pincode format (must be a 6-digit number)")
    private String pincode;

    @NotBlank(message = "State cannot be blank")
    private String state;

    private String landmark; // Optional landmark

    @ManyToOne
    @JsonBackReference("consumer-addresses")
    private Consumer consumer;

    @OneToOne
    @JsonBackReference("delivery-order")  // Change to @JsonBackReference
    private Order order;
}
