package com.example.finalyearproject.Abstraction;

import com.example.finalyearproject.DataStore.Order;
import com.example.finalyearproject.DataStore.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepo extends JpaRepository<Order, Integer> {
    // Find active cart (order with CREATED status) for a consumer
    @Query("SELECT o FROM Order o WHERE o.consumer.consumerId = :consumerId AND o.orderStatus = 'CREATED'")
    Order findActiveCartByConsumerId(@Param("consumerId") int consumerId);

//    // Legacy method - compatible with your existing code
//    @Query("SELECT o FROM Order o WHERE o.consumer.consumerId = :consumerId AND o.orderStatus = 'CREATED'")
//    Order getConsumersCart(@Param("consumerId") int consumerId);

    // Find order by status and consumer ID - for compatibility
    @Query("SELECT o FROM Order o WHERE o.orderStatus = :status AND o.consumer.consumerId = :consumerId")
    Order findByStatusAndConsumerId(@Param("status") String status, @Param("consumerId") int consumerId);

    // Find consumer's orders
    List<Order> findByConsumer_ConsumerIdOrderByCreatedAtDesc(int consumerId);

//    // Find orders by status
//    List<Order> findByOrderStatus(OrderStatus status);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi JOIN oi.product p JOIN p.farmer f " +
            "WHERE f.farmerEmail = :farmerEmail AND o.orderStatus = 'PLACED'")
    List<Order> findPlacedOrdersContainingFarmerProducts(@Param("farmerEmail") String farmerEmail);
}