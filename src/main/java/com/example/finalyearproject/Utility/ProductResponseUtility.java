package com.example.finalyearproject.Utility;

import lombok.Builder;
import lombok.Data;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ProductResponseUtility {
    private int productId;
    private String name;
    private String description;
    private double price;
    private int stock;
    private String category;
    private LocalDate harvestDate;
    private LocalDate availableDate;
    private List<String> imageUrls;
}
