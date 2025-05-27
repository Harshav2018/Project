package com.example.finalyearproject.Specifications;

import com.example.finalyearproject.DataStore.CategoryType;
import com.example.finalyearproject.DataStore.Product;
import com.example.finalyearproject.Utility.ProductFilterDTO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> getFilteredProducts(ProductFilterDTO filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search by query (name or description)
            if (filter.getQuery() != null && !filter.getQuery().isEmpty()) {
                String searchPattern = "%" + filter.getQuery().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchPattern)
                ));
            }

            // Filter by category
            if (filter.getCategory() != null && !filter.getCategory().isEmpty()) {
                try {
                    CategoryType category = CategoryType.valueOf(filter.getCategory().toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("category"), category));
                } catch (IllegalArgumentException ignored) {
                    // Invalid category, ignore this filter
                }
            }

            // Filter by price range
            if (filter.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
            }

            // Filter by quantity/stock range
            if (filter.getMinStock() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("stock"), filter.getMinStock()));
            }
            if (filter.getMaxStock() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("stock"), filter.getMaxStock()));
            }

            // Filter by availability date
            if (filter.getAvailableFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("availableFromDate"), filter.getAvailableFrom()));
            }
            if (filter.getAvailableTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("availableFromDate"), filter.getAvailableTo()));
            }

            // Filter by organic
            if (filter.getOrganic() != null) {
                predicates.add(criteriaBuilder.equal(root.get("organic"), filter.getOrganic()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}