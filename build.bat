@echo off
title Build Credit Risk System
echo Compiling Java 25 sources...
javac -d bin src/com/creditrisk/*.java src/com/creditrisk/model/*.java src/com/creditrisk/engine/*.java src/com/creditrisk/repository/*.java src/com/creditrisk/server/*.java src/com/creditrisk/cli/*.java src/com/creditrisk/util/*.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)
echo Packaging CreditRiskSystem.jar...
echo Main-Class: com.creditrisk.Main > manifest.txt
echo. >> manifest.txt
"C:\Program Files\Java\jdk-25.0.2\bin\jar.exe" cfm CreditRiskSystem.jar manifest.txt -C bin .
del manifest.txt
echo Build completed successfully: CreditRiskSystem.jar
pause
