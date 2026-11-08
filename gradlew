#!/usr/bin/env sh
# Small self-bootstrapping Gradle launcher. The standard wrapper JAR was absent
# from the supplied archive, so this script downloads the pinned distribution.
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION="9.8"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-${HOME}/.gradle}"
DIST_ROOT="${GRADLE_USER_HOME}/wrapper/dists/gradle-${GRADLE_VERSION}-bin/manual"
GRADLE_BIN="${DIST_ROOT}/gradle-${GRADLE_VERSION}/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
    mkdir -p "$DIST_ROOT"
    ZIP_FILE="${DIST_ROOT}/gradle-${GRADLE_VERSION}-bin.zip"
    URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

    echo "Gradle ${GRADLE_VERSION} was not found; downloading it..."
    if command -v curl >/dev/null 2>&1; then
        curl -fL --retry 3 --connect-timeout 20 "$URL" -o "$ZIP_FILE"
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$ZIP_FILE" "$URL"
    else
        echo "Install curl or wget to download Gradle ${GRADLE_VERSION}." >&2
        exit 1
    fi

    if command -v unzip >/dev/null 2>&1; then
        unzip -q -o "$ZIP_FILE" -d "$DIST_ROOT"
    else
        echo "Install unzip to extract Gradle ${GRADLE_VERSION}." >&2
        exit 1
    fi
    rm -f "$ZIP_FILE"
fi

cd "$APP_HOME"
exec "$GRADLE_BIN" "$@"
