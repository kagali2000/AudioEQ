#!/usr/bin/env sh
echo "Gradle wrapper placeholder. On CI the runner will install Gradle or use system gradle."
if [ -f "./gradlew" ] && [ "$0" != "./gradlew" ]; then
  exec ./gradlew "$@"
else
  echo "No gradle wrapper jar present; attempting to use system gradle..."
  gradle "$@"
fi
