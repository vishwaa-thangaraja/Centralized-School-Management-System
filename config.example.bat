@echo off
set "CSMS_DB_USER=CSMS"
set "CSMS_DB_PASSWORD=your_database_password"
set "CSMS_DB_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1"
set "CSMS_JAVAFX_LIB=E:\JavaFX\javafx-sdk-17.0.18\lib"

rem Optional: only needed for forgot-password OTP email.
rem set "CSMS_EMAIL=your_email@gmail.com"
rem set "CSMS_EMAIL_PASSWORD=your_gmail_app_password"
