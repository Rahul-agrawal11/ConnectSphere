package com.connectsphere.auth.service;

import com.connectsphere.auth.service.impl.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    RedisTemplate<String, String> redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    @InjectMocks
    OtpService otpService;

    @Test
    void generateAndStoreOtp_shouldReturnSixDigitOtp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String otp = otpService.generateAndStoreOtp("rahul@gmail.com");
        assertThat(otp).matches("\\d{6}");
        verify(valueOperations).set("OTP: rahul@gmail.com", otp, 5L, TimeUnit.MINUTES);
    }

    @Test
    void generateAndStoreOtp_shouldGenerateDifferentOtpsEachTime() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String otp1 = otpService.generateAndStoreOtp("a@test.com");
        String otp2 = otpService.generateAndStoreOtp("b@test.com");
        assertThat(otp1).matches("\\d{6}");
        assertThat(otp2).matches("\\d{6}");
    }

    @Test
    void storePendingUser_shouldSaveWithCorrectKeyAndTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        otpService.storePendingUser("rahul@gmail.com", "{\"username\":\"rahul\"}");
        verify(valueOperations).set("PENDING_USER:rahul@gmail.com", "{\"username\":\"rahul\"}", 10L, TimeUnit.MINUTES);
    }

    @Test
    void getPendingUser_shouldReturnStoredData() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("PENDING_USER:rahul@gmail.com")).thenReturn("{\"username\":\"rahul\"}");
        assertThat(otpService.getPendingUser("rahul@gmail.com")).isEqualTo("{\"username\":\"rahul\"}");
    }

    @Test
    void getPendingUser_whenNotFound_shouldReturnNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("PENDING_USER:unknown@gmail.com")).thenReturn(null);
        assertThat(otpService.getPendingUser("unknown@gmail.com")).isNull();
    }

    @Test
    void validateOtp_withCorrectOtp_shouldReturnTrueAndDeleteKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("OTP: rahul@gmail.com")).thenReturn("123456");
        when(redisTemplate.delete("OTP: rahul@gmail.com")).thenReturn(true);
        assertThat(otpService.validateOtp("rahul@gmail.com", "123456")).isTrue();
        verify(redisTemplate).delete("OTP: rahul@gmail.com");
    }

    @Test
    void validateOtp_withWrongOtp_shouldReturnFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("OTP: rahul@gmail.com")).thenReturn("123456");
        assertThat(otpService.validateOtp("rahul@gmail.com", "999999")).isFalse();
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void validateOtp_withExpiredOrMissingOtp_shouldReturnFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("OTP: rahul@gmail.com")).thenReturn(null);
        assertThat(otpService.validateOtp("rahul@gmail.com", "123456")).isFalse();
    }

    @Test
    void deletePendingUser_shouldDeleteCorrectKey() {
        otpService.deletePendingUser("rahul@gmail.com");
        verify(redisTemplate).delete("PENDING_USER:rahul@gmail.com");
    }
}