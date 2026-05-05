package com.connectsphere.auth.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpEmailEvent {

    private String type;   // Always "OTP_EMAIL"
    private String toEmail;
    private String userName;
    private String otp;
}
