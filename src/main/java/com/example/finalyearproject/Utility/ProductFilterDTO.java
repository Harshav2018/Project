package com.example.finalyearproject.Utility;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class ProductFilterDTO {
    // Search text
    private String query;

    // Category filter
    private String category;

    // Price range
    private Double minPrice;
    private Double maxPrice;

    // Quantity/Stock range
    private Integer minStock;
    private Integer maxStock;

    // Availability date range
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date availableFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date availableTo;

    // Unit type
    private String unit;

    // Organic filter
    private Boolean organic;

    // Sorting
    private String sortBy; // Options: price_asc, price_desc, name_asc, name_desc, date_asc, date_desc

    // Pagination
    private Integer page = 0;
    private Integer size = 10;
}