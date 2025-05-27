package com.example.finalyearproject.Abstraction;

import com.example.finalyearproject.DataStore.OrderItem;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepo extends JpaRepository<OrderItem, Integer> {

    @Query("select o from OrderItem o where o.order.consumer.consumerId=:#{#consumerId} and o.order.orderStatus='CREATED' and o.orderItemId=:#{#orderItemId}")
    OrderItem findOrderItemWithStatusCREATED(int consumerId, int orderItemId);

    @Modifying
    @Transactional
    @Query("delete from OrderItem o where o.orderItemId=:#{#orderItemId}")
    void deleteByOrderItemId(int orderItemId);

    // Find order items for a specific product and consumer
    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o " +
            "WHERE oi.product.productId = :productId AND o.consumer.consumerId = :consumerId")
    List<OrderItem> findByProductIdAndConsumerId(
            @Param("productId") int productId,
            @Param("consumerId") int consumerId);
}