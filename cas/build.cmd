@echo off

@set JAVA_ARGS=-Xms500m -Xmx1g
@set CAS_DIR=\etc\cas
@set CONFIG_DIR=\etc\cas\config
@set SHELL_DIR=build\libs
@set BUILD_DIR=build\libs
@set DOCKER_ORG=org.apereo.cas
@if "%PROFILES%" == "" @set PROFILES=standalone

@rem Call this script with DNAME and CERT_SUBJ_ALT_NAMES already set to override
@if "%DNAME%" == "" set DNAME=CN=cas.example.org,OU=Example,OU=Org,C=US
@rem List other host names or ip addresses you want in your certificate, may help with host name verification, 
@rem   if client apps make https connection for ticket validation and compare name in cert (include sub. alt. names) 
@rem   to name used to access CAS
@if "%CERT_SUBJ_ALT_NAMES%" == "" set CERT_SUBJ_ALT_NAMES=dns:example.org,dns:localhost,dns:%COMPUTERNAME%,ip:127.0.0.1

@rem Check for gradle in path, use it if found, otherwise use gradle wrapper
@set GRADLE_CMD=gradle
@where /q gradle
@if %ERRORLEVEL% neq 0 set GRADLE_CMD=.\gradlew.bat

@if "%1" == "" call:help
@if "%1" == "clean" (
    call:clean
    shift
)
@if "%1" == "copy" call:copy
@if "%1" == "package" call:package %2 %3 %4
@if "%1" == "debug" call:debug %2 %3 %4
@if "%1" == "run" call:run %2 %3 %4 & goto :EOF
@if "%1" == "refresh" call:refresh %2 %3 %4
@if "%1" == "help" call:help
@if "%1" == "gencert" call:gencert
@if "%1" == "cli" call:cli
@if "%1" == "debugcli" call:debugcli
@if "%1" == "dependencies" call:dependencies
@if "%1" == "dockerimage" call:dockerimage
@if "%1" == "dockerrun" call:dockerrun
@if "%1" == "dockerrunsh" call:dockerrunsh
@if "%1" == "dockerexecsh" call:dockerexecsh

@rem function section starts here
@goto :EOF

:copy
    @echo "Creating configuration directory under %CONFIG_DIR%"
    if not exist %CONFIG_DIR% mkdir %CONFIG_DIR%

    @echo "Copying configuration files from etc/cas to /etc/cas"
    xcopy /S /Y etc\cas\* \etc\cas
@goto :EOF

:help
    @echo "Usage: build.cmd [copy|clean|package|refresh|run|debug|gencert|dockerimage|dockerrunsh|dockerexecsh] [optional extra args for gradle]"
    @echo "To get started on a clean system, run 'build.cmd gencert && build.cmd copy && build.cmd run'"
    @echo "Note that using the copy or gencert arguments will create and/or overwrite the %CAS_DIR% which is outside this project"
@goto :EOF

:clean
    call %GRADLE_CMD% clean %1 %2 %3
    exit /B %ERRORLEVEL%
@goto :EOF

:package
    call %GRADLE_CMD% build %1 %2 %3
    exit /B %ERRORLEVEL%
@goto :EOF

:debug
    call:package %1 %2 %3 & java %JAVA_ARGS% -Xdebug -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n -jar %BUILD_DIR%\cas.war --spring.profiles.active=%PROFILES%
@goto :EOF

:run
    call:package %1 %2 %3 & java %JAVA_ARGS% -jar %BUILD_DIR%\cas.war --spring.profiles.active=%PROFILES%
@goto :EOF

:refresh
    call:package --refresh-dependencies %1 %2
@goto :EOF

:dockerimage
    call %GRADLE_CMD% clean build jibDockerBuild
@goto :EOF

:dockerrun
    docker stop cas
    docker rm cas
    docker run --name cas %DOCKER_ORG%/cas:latest
@goto :EOF

:dockerrunsh
    @rem run image to look around, delete container on exit
    docker run --rm -it --entrypoint /bin/sh %DOCKER_ORG%/cas:latest
@goto :EOF

:dockerexecsh
    @rem exec into runing container to look around, run jstack, check config, etc
    docker exec -it cas /bin/sh
@goto :EOF


:gencert
    where /q keytool
    if ERRORLEVEL 1 (
        @echo Java keytool.exe not found in path. 
        exit /b 1
    ) else (
        if not exist %CAS_DIR% mkdir %CAS_DIR%
        @echo on
        @echo Generating self-signed SSL cert for %DNAME% in %CAS_DIR%\thekeystore
        keytool -genkeypair -alias cas -keyalg RSA -keypass changeit -storepass changeit -keystore %CAS_DIR%\thekeystore -dname %DNAME% -ext SAN=%CERT_SUBJ_ALT_NAMES%
        @echo Exporting cert for use in trust store (used by cas clients)
        keytool -exportcert -alias cas -storepass changeit -keystore %CAS_DIR%\thekeystore -file %CAS_DIR%\cas.cer
    )
@goto :EOF

:cli 
call %GRADLE_CMD% runShell
@goto :EOF

:debugcli 
call %GRADLE_CMD% debugShell
@goto :EOF

:dependencies
call %GRADLE_CMD% allDependencies
@goto :EOF

