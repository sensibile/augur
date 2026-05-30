#!/bin/sh

APP_HOME=$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd -P)

exec java \
  -Dorg.gradle.appname=gradlew \
  -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
