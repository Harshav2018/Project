package com.example.finalyearproject.Controllers;
import com.example.finalyearproject.Abstraction.FarmerRepo;
import com.example.finalyearproject.DataStore.Consumer;
import com.example.finalyearproject.DataStore.Farmer;
import com.example.finalyearproject.Model.JwtRequest;
import com.example.finalyearproject.Model.JwtResponse;
import com.example.finalyearproject.Security.JwtHelper;
import com.example.finalyearproject.Services.ConsumerService;
import com.example.finalyearproject.Services.FarmerService;
import com.example.finalyearproject.Utility.ApiResponse;
import com.example.finalyearproject.Utility.ConsumerRegisterDTO;
import com.example.finalyearproject.Utility.FarmerRegisterDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public")
public class PublicController {

    @Autowired
    private ConsumerService consumerService;

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private AuthenticationManager manager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private FarmerRepo farmerRepo;

    @Autowired
    private FarmerService farmerService;

    @PostMapping(path = "/register-consumer")
    public ResponseEntity<ApiResponse<Consumer>> RegisterConsumer(@Valid @ModelAttribute ConsumerRegisterDTO consumerRegisterDTO) {
        ApiResponse<Consumer> response = consumerService.RegisterConsumer(consumerRegisterDTO);

        if (response.getData() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }
    }

    @RequestMapping(value = "/login-consumer", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<ApiResponse<JwtResponse>> loginConsumer(@RequestBody JwtRequest request) {
        try {
            this.doAuthenticate(request.getUserEmail(), request.getUserPassword());

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUserEmail());

            // Get the role (first authority)
            String role = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)  // Use lambda instead of method reference
                    .findFirst()
                    .orElse("UNKNOWN");

            String token = this.jwtHelper.generateToken(userDetails);

            JwtResponse response = JwtResponse.builder()
                    .jwtToken(token)
                    .userName(userDetails.getUsername())
                    .role(role) // Include role in response
                    .build();

            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (BadCredentialsException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication failed", "Invalid username or password"));
        }
    }


    private void doAuthenticate(String userName, String password) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userName, password);
        try {
            manager.authenticate(authentication);
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    @PostMapping("/register-farmer")
    public ResponseEntity<ApiResponse<Farmer>> RegisterFarmer(@Valid @ModelAttribute FarmerRegisterDTO farmerRegisterDTO) {
        ApiResponse<Farmer> response = farmerService.RegisterFarmer(farmerRegisterDTO);

        if (response.getData() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }
    }

    @RequestMapping(value = "/login-farmer", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<ApiResponse<JwtResponse>> loginFarmer(@RequestBody JwtRequest request) {
        try {
            this.doAuthenticate(request.getUserEmail(), request.getUserPassword());

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUserEmail());
            String userName;

            try {
                Farmer farmer = this.farmerRepo.findFarmerByFarmerEmail(request.getUserEmail())
                        .orElseThrow();
                userName = farmer.getFarmerName();

                // Get the role (first authority)
                String role = userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)  // Use lambda instead of method reference
                        .findFirst()
                        .orElse("UNKNOWN");

                String token = this.jwtHelper.generateToken(userDetails);

                JwtResponse response = JwtResponse.builder()
                        .jwtToken(token)
                        .userName(userName)
                        .role(role) // Include role in response
                        .build();

                return ResponseEntity.ok(ApiResponse.success("Login successful", response));
            } catch (Exception e) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication failed", "Farmer not found with provided email"));
            }
        } catch (BadCredentialsException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication failed", "Invalid username or password"));
        }
    }

}