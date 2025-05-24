#!/usr/bin/env sh

#
# Copyright 2011-2016 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses "$JAVA_HOME/jre/sh/java" for its java command
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
        echo "Please set the JAVA_HOME environment variable in your environment to point to the Java installation directory."
        exit 1
    fi
else
    JAVACMD="java"
    which java >/dev/null || { echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."; echo "Please set the JAVA_HOME environment variable in your environment to point to the Java installation directory."; exit 1; }
fi

# Determine the script directory.
SCRIPT_DIR="$( cd "$( dirname "$0" )" && pwd )"

# Read the wrapper configuration.
WRAPPER_PROPERTIES="$SCRIPT_DIR"/gradle/wrapper/gradle-wrapper.properties
if [ ! -f "$WRAPPER_PROPERTIES" ] ; then
    echo "ERROR: Could not find wrapper properties file: $WRAPPER_PROPERTIES"
    exit 1
fi

# Determine the wrapper JAR file.
WRAPPER_JAR="$SCRIPT_DIR"/gradle/wrapper/gradle-wrapper.jar
if [ ! -f "$WRAPPER_JAR" ] ; then
    echo "ERROR: Could not find gradle wrapper within Gradle distribution."
    echo "Please ensure you have downloaded the full distribution or run './gradlew wrapper --gradle-version <version>' to generate it."
    exit 1
fi

# Define the Gradle parameters (for version resolution)
# This part is mostly informational for the user, not for actual downloading.
# The real download/unpacking happens when org.gradle.wrapper.GradleWrapperMain is executed.
if [ -f "$WRAPPER_PROPERTIES" ]; then
  GRADLE_VERSION=$(grep 'distributionUrl' "$WRAPPER_PROPERTIES" | sed -n 's/.*gradle-\([0-9\.]*\)-\(all\|bin\)\.zip/\1/p')
fi

# Now execute the wrapper JAR.
exec "$JAVACMD" $JAVA_OPTS $GRADLE_OPTS -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
