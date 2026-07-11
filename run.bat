@echo off
setlocal
cd /d "%~dp0"

if /i "%~1"=="--worker" goto worker
if /i "%~1"=="--launch" goto launch

start "" powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "%~dp0tools\startup_loader.ps1" "%CD%"
exit /b 0

:worker
if not exist "config.local.bat" (
    echo Missing config.local.bat.
    echo Copy config.example.bat to config.local.bat and update your local DB credentials.
    exit /b 1
)

call "config.local.bat"

if "%CSMS_EMAIL%"=="" (
    echo [WARN] OTP email is not configured.
    echo [WARN] Set CSMS_EMAIL and CSMS_EMAIL_PASSWORD in config.local.bat to enable forgot-password OTP.
) else if "%CSMS_EMAIL_PASSWORD%"=="" (
    echo [WARN] OTP email password is not configured.
    echo [WARN] Set CSMS_EMAIL_PASSWORD in config.local.bat to enable forgot-password OTP.
)

if exist out rd /s /q out
mkdir out
mkdir out\view
mkdir out\css
mkdir out\assets
xcopy /y /s /i src\view out\view
xcopy /y /s /i src\css out\css
if exist src\assets xcopy /y /s /i src\assets out\assets
if exist src\assets xcopy /y src\assets\* out\

javac -encoding UTF-8 --module-path "%CSMS_JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml,javafx.media -cp "lib/*;out" -d out src\model\*.java src\dao\*.java src\service\*.java src\controller\*.java src\main\Main.java
if errorlevel 1 exit /b 1

exit /b 0

:launch
if not exist "config.local.bat" (
    echo Missing config.local.bat.
    echo Copy config.example.bat to config.local.bat and update your local DB credentials.
    exit /b 1
)

call "config.local.bat"

start "" javaw --module-path "%CSMS_JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml,javafx.media -cp "out;lib/*" main.Main
exit /b 0
