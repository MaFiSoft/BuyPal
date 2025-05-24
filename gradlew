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
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME environment variable in your environment to point to the Java installation directory."
    fi
else
    JAVACMD="java"
    which java >/dev/null || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME environment variable in your environment to point to the Java installation directory."
fi

# Determine the script directory.
SCRIPT_DIR="$( cd "$( dirname "$0" )" && pwd )"

# Read the wrapper configuration.
WRAPPER_PROPERTIES="$SCRIPT_DIR"/gradle/wrapper/gradle-wrapper.properties
if [ ! -f "$WRAPPER_PROPERTIES" ] ; then
    die "ERROR: Could not find wrapper properties file: $WRAPPER_PROPERTIES"
fi

# Determine the wrapper JAR file.
WRAPPER_JAR="$SCRIPT_DIR"/gradle/wrapper/gradle-wrapper.jar

# This is the path of gradlew
GRADLE_OPTS="$GRADLE_OPTS -Dorg.gradle.daemon=true"

# Define the Gradle parameters (for version resolution)
if [ -f "$WRAPPER_PROPERTIES" ]; then
  GRADLE_VERSION=$(grep 'distributionUrl' "$WRAPPER_PROPERTIES" | sed -n 's/.*gradle-\([0-9\.]*\)-\(all\|bin\)\.zip/\1/p')
fi

# Determine the download URL from gradle-wrapper.properties if the jar is missing
if [ ! -f "$WRAPPER_JAR" ]; then
    DISTRIBUTION_URL=$(grep 'distributionUrl' "$WRAPPER_PROPERTIES" | sed 's/distributionUrl=\(.*\)/\1/')
    DISTRIBUTION_FILENAME=$(basename "$DISTRIBUTION_URL")
    LOCAL_DISTRIBUTION_PATH="$SCRIPT_DIR"/gradle/wrapper/"$DISTRIBUTION_FILENAME"

    if [ ! -f "$LOCAL_DISTRIBUTION_PATH" ]; then
        echo "Downloading Gradle distribution from: $DISTRIBUTION_URL"
        if command -v curl >/dev/null 2>&1; then
            curl -L -o "$LOCAL_DISTRIBUTION_PATH" "$DISTRIBUTION_URL"
        elif command -v wget >/dev/null 2>&1; then
            wget -O "$LOCAL_DISTRIBUTION_PATH" "$DISTRIBUTION_URL"
        else
            die "ERROR: Neither curl nor wget found. Cannot download Gradle distribution."
        fi
        if [ $? -ne 0 ]; then
            die "ERROR: Failed to download Gradle distribution from $DISTRIBUTION_URL"
        fi
    fi

    # Unpack the distribution if it's a zip file
    if [[ "$LOCAL_DISTRIBUTION_PATH" == *.zip ]]; then
        echo "Unpacking Gradle distribution..."
        UNPACK_DIR="$SCRIPT_DIR"/gradle/wrapper/dists/gradle-"$GRADLE_VERSION"
        mkdir -p "$UNPACK_DIR"
        unzip -q "$LOCAL_DISTRIBUTION_PATH" -d "$UNPACK_DIR"
        if [ $? -ne 0 ]; then
            die "ERROR: Failed to unpack Gradle distribution."
        fi
    fi
fi

# Now execute the wrapper JAR.
exec "$JAVACMD" $JAVA_OPTS $GRADLE_OPTS -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
