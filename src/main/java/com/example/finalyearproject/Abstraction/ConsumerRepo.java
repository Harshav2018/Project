package com.example.finalyearproject.Abstraction;

import com.example.finalyearproject.DataStore.Consumer;
import com.example.finalyearproject.DataStore.DeliveryAddresses;
import com.example.finalyearproject.DataStore.OrderItem;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
@EnableJpaRepositories
public interface ConsumerRepo extends JpaRepository<Consumer, Integer> {

    Consumer findConsumerByConsumerId(int id);
    @Modifying
    @Transactional
    @Query(value = "update Consumer set consumer_First_Name=:#{#consumer.consumerFirstName}, consumer_Last_Name=:#{#consumer.consumerLastName}, " +
            "consumer_Phone=:#{#consumer.consumerPhone}, consumer_Address=:#{#consumer.consumerAddress} where consumer_Id=:#{#id}",nativeQuery = true)
     void updateConsumerByconsumerId(Consumer consumer,int id);

    @Modifying
    @Transactional
    @Query("update DeliveryAddresses set streetAddress=:#{#deliveryAddresses.streetAddress}, city=:#{#deliveryAddresses.city}, pincode=:#{#deliveryAddresses.pincode}," +
            "state=:#{#deliveryAddresses.state}, landmark=:#{#deliveryAddresses.landmark} where deliveryAddressId=:#{#addressId} and consumer.consumerId=:#{#consumerId}")
     void updateDeliveryAddress(DeliveryAddresses deliveryAddresses,int addressId,int consumerId);

    @Modifying
    @Transactional
    @Query("delete from DeliveryAddresses where deliveryAddressId=:#{#addressId} and consumer.consumerId=:#{#consumerId}")
     void deleteDeliveryAddressById(int addressId,int consumerId);

    Consumer findByConsumerEmail(String consumerEmail);

    Optional<Consumer> findConsumerByConsumerEmail(String consumerEmail);
}
