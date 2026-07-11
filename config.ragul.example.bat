@echo off
rem Local profile for Ragul's system.
rem Copy this file to config.local.bat, then fill in the local password values.

set "CSMS_DB_USER=C##CSMS"
set "CSMS_DB_PASSWORD=your_oracle_password"
set "CSMS_DB_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1"
set "CSMS_JAVAFX_LIB=C:\javafx\javafx-sdk-17.0.19\lib"

rem Optional: only needed for forgot-password OTP email.
rem set "CSMS_EMAIL=securitycsms@gmail.com"
rem set "CSMS_EMAIL_PASSWORD=your_gmail_app_password"
