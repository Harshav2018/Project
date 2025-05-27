package com.example.finalyearproject.Abstraction;

import com.example.finalyearproject.DataStore.Farmer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;

@EnableJpaRepositories
public interface FarmerRepo extends JpaRepository<Farmer,Integer> {
     Optional<Farmer> findFarmerByFarmerEmail(String farmerName);


     Optional<Farmer> findByFarmerId(int farmerId);

    @Modifying
    @Query("update Farmer set firstName=:#{#farmer.firstName}, lastName=:#{#farmer.lastName}, farmerAddress=:#{#farmer.farmerAddress}," +
            " farmerPhone=:#{#farmer.farmerPhone} where farmerId=:#{#farmerId}")
     void updateByFarmerId(Farmer farmer,int farmerId);

    Farmer findByFarmerEmail(String farmerEmail);
}
