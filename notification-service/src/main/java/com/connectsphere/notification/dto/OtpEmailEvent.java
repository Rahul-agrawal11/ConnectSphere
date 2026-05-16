package com.connectsphere.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpEmailEvent {

    private String type;
    private String toEmail;
    private String userName;
    private String otp;
}
