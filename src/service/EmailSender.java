package service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailSender {
    private String lastErrorMessage = "";

    public boolean sendOTP(String recipientEmail, String otp) {
        String configuredSenderEmail = resolveConfig("CSMS_EMAIL", "CSMS_SMTP_EMAIL");
        String schoolEmail = SchoolSettingsService.getSchoolEmail();
        final String senderEmail = firstNonBlank(configuredSenderEmail, schoolEmail);
        final String senderPassword = resolveConfig("CSMS_EMAIL_PASSWORD", "CSMS_EMAIL_APP_PASSWORD", "CSMS_SMTP_PASSWORD");

        if (senderEmail == null || senderEmail.isBlank() || senderPassword == null || senderPassword.isBlank()) {
            lastErrorMessage = "OTP email is not configured. Set the school email in Settings and CSMS_EMAIL_PASSWORD in config.local.bat.";
            System.err.println(lastErrorMessage);
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            if (configuredSenderEmail != null && schoolEmail != null && !schoolEmail.isBlank()
                    && !configuredSenderEmail.equalsIgnoreCase(schoolEmail)) {
                message.setReplyTo(InternetAddress.parse(schoolEmail));
            }
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(SchoolSettingsService.getSchoolName() + " Password Reset - OTP");
            message.setText("Your OTP for password reset is: " + otp + "\nThis OTP is valid for 5 minutes.");

            Transport.send(message);
            lastErrorMessage = "";
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            String rawMessage = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (rawMessage.contains("authentication") || rawMessage.contains("username and password not accepted")) {
                lastErrorMessage = "Email login failed. Check CSMS_EMAIL and Gmail app password.";
            } else {
                lastErrorMessage = "SMTP connection failed. Check internet or Gmail SMTP access.";
            }
            return false;
        }
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    private String resolveConfig(String... keys) {
        for (String key : keys) {
            String envValue = System.getenv(key);
            if (envValue != null && !envValue.isBlank()) {
                return envValue.trim();
            }

            String propertyValue = System.getProperty(key);
            if (propertyValue != null && !propertyValue.isBlank()) {
                return propertyValue.trim();
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
