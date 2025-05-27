package com.example.finalyearproject.Abstraction;

import com.example.finalyearproject.DataStore.CategoryType;
import com.example.finalyearproject.DataStore.Order;
import com.example.finalyearproject.DataStore.Product;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepo extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {

    @Query("select p from Product p where p.productId=:#{#productId}")
    Product findProductByProductId(int productId);

    @Modifying
    @Transactional
    @Query("update Product p set p.name=:#{#product.name}, p.description=:#{#product.description}, p.price=:#{#product.price}" +
            ", p.stock=:#{#product.stock} where p.productId=:#{#productId} and p.farmer.farmerId=:#{#farmerId}")
    void updateProductById(Product product, int productId, int farmerId);

    @Modifying
    @Query("delete Product where productId=:#{#productId} and farmer.farmerId=:#{#farmerId}")
    void deleteByProductId(int productId, int farmerId);

    List<Product> findByCategory(CategoryType category);

    Optional<Product> findByFarmer_FarmerIdAndProductId(int farmerId, int productId);

    List<Product> findByFarmer_FarmerId(int farmerId);

    List<Product> findByNameContainingIgnoreCase(String query);

    List<Product> findTop10ByOrderByAverageRatingDesc();

    List<Product> findTop10ByOrderByProductIdDesc();

    Optional<Product> findByProductId(int productId);

    // Get all product IDs (for shuffling)
    @Query(value = "SELECT productId FROM Product WHERE stock > 0")
    List<Integer> findAllAvailableProductIds();

    // Get products by specific IDs in the given order
    @Query("SELECT p FROM Product p WHERE p.productId IN :ids ORDER BY FIELD(p.productId, :ids)")
    List<Product> findByProductIdsInOrder(List<Integer> ids);

    // MySQL specific version of the query above
    @Query(value = "SELECT * FROM product WHERE product_id IN ?1 ORDER BY FIELD(product_id, ?1)", nativeQuery = true)
    List<Product> findByProductIdsInOrderNative(List<Integer> ids);

}