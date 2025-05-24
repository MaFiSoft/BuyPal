@rem gemini
@rem Copyright 2011-2016 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Gradle start up script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem Add default JVM options here. You may use JAVA_OPTS and GRADLE_OPTS as well.
set DEFAULT_JVM_OPTS="-Xmx64m"

set DIR=%~dp0
if "%DIR%" == "" set DIR=%CD%
set APP_BASE_NAME=%~n0
set APP_HOME=%DIR%

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
for %%i in (%PATH%) do (
  if exist "%%i\%JAVA_EXE%" (
    set JAVA_EXE="%%i\%JAVA_EXE%"
    goto foundJava
  )
)

:findJavaFromJavaHome
set JAVA_EXE="%JAVA_HOME%\bin\java.exe"
if exist %JAVA_EXE% goto foundJava

echo.
echo ERROR: JAVA_HOME is set to an invalid directory.
echo JAVA_HOME = %JAVA_HOME%
echo Please set the JAVA_HOME environment variable in your environment to point to the Java installation directory.
goto fail

:foundJava
if exist "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" goto init

echo.
echo ERROR: Could not find gradle wrapper within Gradle distribution.
echo Please ensure you have downloaded the full distribution.
echo.
goto fail

:init
@rem Get command-line arguments, if any
set CMD_LINE_ARGS=%*

call "%APP_HOME%\gradle\wrapper\gradle-wrapper.properties" :setWrapperProperties
@rem Wrapper properties defined

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

@rem Setup Gradle properties for version resolution if the jar is missing
if not exist "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" (
    for /f "tokens=*" %%a in ('findstr /b "distributionUrl=" "%APP_HOME%\gradle\wrapper\gradle-wrapper.properties"') do (
        set "DISTRIBUTION_URL=%%a"
    )
    set "DISTRIBUTION_URL=%DISTRIBUTION_URL:distributionUrl=%"
    set "DISTRIBUTION_FILENAME="
    for %%f in ("%DISTRIBUTION_URL%") do (
        set "DISTRIBUTION_FILENAME=%%~nxf"
    )
    set "LOCAL_DISTRIBUTION_PATH=%APP_HOME%\gradle\wrapper\%DISTRIBUTION_FILENAME%"

    if not exist "%LOCAL_DISTRIBUTION_PATH%" (
        echo Downloading Gradle distribution from: %DISTRIBUTION_URL%
        curl -L -o "%LOCAL_DISTRIBUTION_PATH%" "%DISTRIBUTION_URL%" || (
            echo ERROR: Failed to download Gradle distribution from %DISTRIBUTION_URL%
            goto fail
        )
    )

    @rem Unpack the distribution if it's a zip file
    if "%LOCAL_DISTRIBUTION_PATH:*.zip=%" NEQ "%LOCAL_DISTRIBUTION_PATH%" (
        echo Unpacking Gradle distribution...
        for /f "tokens=1 delims=-" %%a in ("%DISTRIBUTION_FILENAME%") do (
            set "GRADLE_VERSION=%%a"
        )
        set "UNPACK_DIR=%APP_HOME%\gradle\wrapper\dists\%GRADLE_VERSION%"
        if not exist "%UNPACK_DIR%" mkdir "%UNPACK_DIR%"
        tar -xf "%LOCAL_DISTRIBUTION_PATH%" -C "%UNPACK_DIR%" || (
            echo ERROR: Failed to unpack Gradle distribution.
            goto fail
        )
    )
)

"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%

:end
@if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
endlocal
exit 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

@rem Helper to extract wrapper properties.
:setWrapperProperties
for /f "tokens=1* delims==" %%A in ('type "%APP_HOME%\gradle\wrapper\gradle-wrapper.properties"') do (
    if /i "%%A"=="distributionUrl" set "_distributionUrl=%%B"
    if /i "%%A"=="distributionBase" set "_distributionBase=%%B"
    if /i "%%A"=="distributionPath" set "_distributionPath=%%B"
    if /i "%%A"=="zipStoreBase" set "_zipStoreBase=%%B"
    if /i "%%A"=="zipStorePath" set "_zipStorePath=%%B"
)
exit /b
