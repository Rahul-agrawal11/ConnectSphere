package com.connectsphere.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String OTP_PREFIX = "OTP: ";
    private static final String PENDING_USER_PREFIX = "PENDING_USER:";
    private static final long OTP_EXPIRY_MINUTES = 5;
    private static final long PENDING_USER_EXPIRY_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateAndStoreOtp(String email) {
        String otp = String.format("%06d", RANDOM.nextInt(999999));

        redisTemplate.opsForValue().set(OTP_PREFIX + email, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);
        log.info("OTP generated for: {}", email);
        return otp;
    }

    public void storePendingUser(String email, String userData){
        redisTemplate.opsForValue().set(PENDING_USER_PREFIX + email, userData, PENDING_USER_EXPIRY_MINUTES, TimeUnit.MINUTES);
    }

    public String getPendingUser(String email) {
        return redisTemplate.opsForValue().get(PENDING_USER_PREFIX + email);
    }

    public boolean validateOtp(String email, String otp) {
        String key = OTP_PREFIX + email;
        String storedOtp = redisTemplate.opsForValue().get(key);
        if(storedOtp != null && storedOtp.equals(otp)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    public void deletePendingUser(String email) {
        redisTemplate.delete(PENDING_USER_PREFIX + email);
    }

}
