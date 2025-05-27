package com.example.finalyearproject.Abstraction;

import com.example.finalyearproject.DataStore.DeliveryAddresses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
public interface DeliveryAddressesRepo extends JpaRepository<DeliveryAddresses,Integer> {

    DeliveryAddresses findDeliveryAddressesByDeliveryAddressId(int deliveryAddressId);
}

