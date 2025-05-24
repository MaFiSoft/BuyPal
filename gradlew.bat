@rem
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
echo Please ensure you have downloaded the full distribution or run 'gradlew wrapper --gradle-version ^<version^>' to generate it.
echo.
goto fail

:init
@rem Get command-line arguments, if any
set CMD_LINE_ARGS=%*

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%

:end
@if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
endlocal
exit 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal
