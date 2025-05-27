package com.example.finalyearproject.Services;

import com.example.finalyearproject.Abstraction.ConsumerRepo;
import com.example.finalyearproject.Abstraction.FarmerRepo;
import com.example.finalyearproject.DataStore.Consumer;
import com.example.finalyearproject.DataStore.Farmer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private ConsumerRepo consumerRepo;

    @Autowired
    private FarmerRepo farmerRepo;

    @Override
    public UserDetails loadUserByUsername(String userEmail) throws UsernameNotFoundException {
        // Try to find the user as a consumer
        Optional<Consumer> consumerOpt = this.consumerRepo.findConsumerByConsumerEmail(userEmail);
        if (consumerOpt.isPresent()) {
            Consumer consumer = consumerOpt.get();
            return new User(
                    consumer.getConsumerEmail(),
                    consumer.getPassword(),
                    Collections.singletonList(new SimpleGrantedAuthority("CONSUMER"))
            );
        }

        // If not a consumer, try to find as a farmer
        Optional<Farmer> farmerOpt = this.farmerRepo.findFarmerByFarmerEmail(userEmail);
        if (farmerOpt.isPresent()) {
            Farmer farmer = farmerOpt.get();
            return new User(
                    farmer.getFarmerEmail(),
                    farmer.getPassword(),
                    Collections.singletonList(new SimpleGrantedAuthority("FARMER"))
            );
        }

        // If user not found in either repository
        throw new UsernameNotFoundException("User not found with email: " + userEmail);
    }
}