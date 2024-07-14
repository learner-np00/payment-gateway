package com.example.demo.dto;

import com.example.demo.entity.CustomerInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KhaltiRequest {
    private String returnUrl;
    private String websiteUrl;
    private Integer amount;
    private String purchaseOrderId;
    private String purchaseOrderName;
    private CustomerInfo customerInfo;
}
