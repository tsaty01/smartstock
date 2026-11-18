#!/usr/bin/env bash
# Stamp the version chosen by semantic-release, then build the server JARs and the signed APKs.
set -euo pipefail

version="${1:?semantic-release version is required}"

sed -i "s/^version=.*/version=${version}/" server/gradle.properties android/gradle.properties
rm -rf server/build/libs android/dist
./server/gradlew -p server jar bootJar

# The standard APK accepts HTTPS servers only; the -http APK also allows a plain-HTTP laptop or LAN server.
apk=android/app/build/outputs/apk/release/smartstock-android-${version}-release.apk
mkdir -p android/dist
./android/gradlew -p android assembleRelease
cp "$apk" "android/dist/smartstock-android-${version}.apk"
./android/gradlew -p android assembleRelease -PallowCleartext=true
cp "$apk" "android/dist/smartstock-android-${version}-http.apk"
