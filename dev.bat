@echo off
rem SPDX-License-Identifier: GPL-3.0-only
rem Copyright (C) 2026 apanzinc
rem
rem HyperVisage debug helper. Requires the phone to be connected over adb.
rem
rem   dev.bat          incremental build + install + launch (most used)
rem   dev.bat i        same as above
rem   dev.bat l        tail module log (Ctrl+C to quit)
rem   dev.bat c        clear log buffer
rem   dev.bat s        screenshot to dev\shots\shot.png
rem   dev.bat r        force-stop and relaunch this module
rem   dev.bat t <pkg>  restart any target app so hooks reload
rem   dev.bat d        list connected devices

setlocal
set "ROOT=%~dp0"
set "JAVA_HOME=C:\Users\apanz\.android-dev\jdk-21.0.2"
set "ANDROID_HOME=C:\Users\apanz\.android-dev\sdk"
set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
set "PKG=dev.apanzinc.hypervisage"
set "ACT=%PKG%/.ui.MainActivity"
set "TAG=HyperVisage"

if not exist "%ADB%" (
    echo [x] adb not found: %ADB%
    exit /b 1
)

if /i "%~1"=="l" goto do_log
if /i "%~1"=="c" goto do_clear
if /i "%~1"=="s" goto do_shot
if /i "%~1"=="r" goto do_restart
if /i "%~1"=="t" goto do_target
if /i "%~1"=="d" goto do_devices
goto do_install

:check_device
"%ADB%" get-state >nul 2>&1
if errorlevel 1 (
    echo [x] no device attached
    echo     usb: enable "USB debugging" in developer options, then run: dev.bat d
    echo     wifi: pair from "Wireless debugging", then:
    echo           adb pair <ip>:<pair-port> <code>
    echo           adb connect <ip>:<debug-port>
    exit /b 1
)
exit /b 0

:do_install
call :check_device
if errorlevel 1 exit /b 1
echo [1/2] building and installing...
call "%ROOT%gradlew.bat" :app:installDebug --console=plain
if errorlevel 1 (
    echo [x] build failed, see errors above
    exit /b 1
)
echo [2/2] launching...
"%ADB%" shell am start -n "%ACT%" >nul
echo [ok] installed and launched. log: dev.bat l
exit /b 0

:do_log
call :check_device
if errorlevel 1 exit /b 1
echo === filtering %TAG% + crashes, Ctrl+C to quit ===
"%ADB%" logcat -v color -s "%TAG%:V" "AndroidRuntime:E" "LSPosed:E"
exit /b 0

:do_clear
"%ADB%" logcat -c
echo [ok] log buffer cleared
exit /b 0

:do_shot
call :check_device
if errorlevel 1 exit /b 1
if not exist "%ROOT%dev\shots" mkdir "%ROOT%dev\shots"
"%ADB%" exec-out screencap -p > "%ROOT%dev\shots\shot.png"
if errorlevel 1 (
    echo [x] screenshot failed
    exit /b 1
)
echo [ok] %ROOT%dev\shots\shot.png
exit /b 0

:do_restart
call :check_device
if errorlevel 1 exit /b 1
"%ADB%" shell am force-stop "%PKG%"
"%ADB%" shell am start -n "%ACT%" >nul
echo [ok] restarted %PKG%
exit /b 0

:do_target
if "%~2"=="" (
    echo usage: dev.bat t com.android.systemui
    exit /b 1
)
call :check_device
if errorlevel 1 exit /b 1
echo restarting %~2 ...
"%ADB%" shell am force-stop "%~2"
"%ADB%" shell monkey -p "%~2" -c android.intent.category.LAUNCHER 1 >nul 2>&1
if errorlevel 1 echo     no launcher icon for this package, open it manually
echo [ok] hooks for %~2 reloaded
exit /b 0

:do_devices
"%ADB%" devices -l
exit /b 0
