package com.example.finalyearproject.Controllers;

import com.example.finalyearproject.DataStore.Donation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/donation")
public class DonationController {

    public ResponseEntity<?> donate(@RequestBody Donation donation){
     return null;
    }
}
