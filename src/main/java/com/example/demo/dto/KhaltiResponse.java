package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KhaltiResponse {
    private String pidx;
    private String payment_url;
    private String expires_at;
    private int expires_in;
    private int user_fee;
}
