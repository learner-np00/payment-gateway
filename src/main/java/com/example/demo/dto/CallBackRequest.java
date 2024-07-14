package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallBackRequest {
    private String pidx;
    private String txnId;
    private double amount;
    private double totalAmount;
    private String status;
    private String mobile;
    private String tidx;
    private String purchaseOrderId;
    private String purchaseOrderName;
    private String transactionId;
}
