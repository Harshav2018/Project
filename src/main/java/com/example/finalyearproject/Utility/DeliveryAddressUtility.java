package com.example.finalyearproject.Utility;

import com.example.finalyearproject.DataStore.DeliveryAddresses;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryAddressUtility {
    private int statusCode;
    private String message;
   private DeliveryAddresses deliveryAddresses;
}
