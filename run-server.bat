@echo off
title AURA Credit Risk Evaluation System - Server
echo ===============================================================================
echo            AURA CREDIT RISK EVALUATION SYSTEM (JAVA 25)
echo ===============================================================================
echo Starting Embedded Web Server on port 8080...
start "" "http://localhost:8080"
java -jar CreditRiskSystem.jar --port 8080
pause
