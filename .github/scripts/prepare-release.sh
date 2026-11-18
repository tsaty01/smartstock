#!/usr/bin/env bash
# Stamp the version chosen by semantic-release, then build the server JARs and the signed APK.
set -euo pipefail

version="${1:?semantic-release version is required}"

sed -i "s/^version=.*/version=${version}/" server/gradle.properties android/gradle.properties
rm -rf server/build/libs android/dist
./server/gradlew -p server jar bootJar
./android/gradlew -p android assembleRelease
mkdir -p android/dist
cp "android/app/build/outputs/apk/release/smartstock-android-${version}-release.apk" "android/dist/smartstock-android-${version}.apk"
