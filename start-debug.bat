@echo off
title Paper Minecraft Server (debug)

rem Zelfde als start.bat, maar IntelliJ kan verbinden op poort 5005 voor debuggen/hotswap
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -Xms2G -Xmx4G -jar server.jar --nogui

pause
