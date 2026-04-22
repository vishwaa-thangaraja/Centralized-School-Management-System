package service;

import java.time.LocalDateTime;
import java.util.Random;

public class OTPGenerator {
    private String lastGeneratedOTP;
    private LocalDateTime expiryTime;

    public String generateOTP() {
        Random random = new Random();
        lastGeneratedOTP = String.format("%06d", random.nextInt(1000000));
        expiryTime = LocalDateTime.now().plusMinutes(5);
        return lastGeneratedOTP;
    }

    public boolean isOTPValid(String inputOTP) {
        return inputOTP.equals(lastGeneratedOTP) && LocalDateTime.now().isBefore(expiryTime);
    }
}