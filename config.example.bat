@echo off
rem Generic local profile template.
rem Copy this file to config.local.bat and update the values for your machine.
rem For ready-made machine-specific templates, see:
rem - config.vishwaa.example.bat
rem - config.ragul.example.bat

set "CSMS_DB_USER=CSMS"
set "CSMS_DB_PASSWORD=your_database_password"
set "CSMS_DB_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1"
set "CSMS_JAVAFX_LIB=C:\path\to\javafx-sdk\lib"

rem Optional: only needed for forgot-password OTP email.
rem IMPORTANT: remove 'rem' from the next 2 lines to enable OTP email.
rem set "CSMS_EMAIL=your_email@gmail.com"
rem set "CSMS_EMAIL_PASSWORD=your_16_char_gmail_app_password"
