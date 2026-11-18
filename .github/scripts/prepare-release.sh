#!/usr/bin/env bash
# Stamp the version chosen by semantic-release and build the plain and fat JARs.
set -euo pipefail

version="${1:?semantic-release version is required}"

sed -i "s/^version=.*/version=${version}/" server/gradle.properties
rm -rf server/build/libs
./server/gradlew -p server jar bootJar
