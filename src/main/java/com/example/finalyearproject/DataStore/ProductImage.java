package com.example.finalyearproject.DataStore;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_image")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // File name of the stored image
    @Column(nullable = false)
    private String filename;

    // The relative or absolute path to the image file
    @Column(nullable = false)
    private String filePath;

    // Many images belong to one product
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    @JsonBackReference("product-images")
    private Product product;
}
