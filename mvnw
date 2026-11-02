#!/bin/sh
# Project-local launcher for a pinned Apache Maven distribution.
set -eu
base=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
url=$(sed -n 's/^distributionUrl=//p' "$base/.mvn/wrapper/maven-wrapper.properties" | tr -d '\r')
[ -n "$url" ] || { echo 'distributionUrl is missing.' >&2; exit 1; }
archive_name=${url##*/}
directory_name=${archive_name%-bin.zip}
cache="${MAVEN_USER_HOME:-$HOME/.m2}/wrapper/dists/$directory_name"
command_path="$cache/$directory_name/bin/mvn"
fetch() {
    if command -v curl >/dev/null 2>&1; then curl --fail --location --silent --show-error "$1" --output "$2"
    elif command -v wget >/dev/null 2>&1; then wget -q "$1" -O "$2"
    else echo 'curl or wget is required for the first Maven download.' >&2; exit 1
    fi
}
if [ ! -f "$command_path" ]; then
    command -v unzip >/dev/null 2>&1 || { echo 'unzip is required for the first Maven download.' >&2; exit 1; }
    mkdir -p "$cache"
    archive="$cache/$archive_name"
    checksum="$archive.sha512"
    trap 'rm -f "$archive" "$checksum"' 0 1 2 15
    echo "Downloading $directory_name (first run only)..."
    fetch "$url" "$archive"
    fetch "$url.sha512" "$checksum"
    expected=$(awk '{print $1; exit}' "$checksum")
    if command -v sha512sum >/dev/null 2>&1; then actual=$(sha512sum "$archive" | awk '{print $1}')
    elif command -v shasum >/dev/null 2>&1; then actual=$(shasum -a 512 "$archive" | awk '{print $1}')
    else echo 'sha512sum or shasum is required.' >&2; exit 1
    fi
    [ "$actual" = "$expected" ] || { echo 'Maven archive SHA-512 verification failed.' >&2; exit 1; }
    unzip -q -o "$archive" -d "$cache"
    rm -f "$archive" "$checksum"
    trap - 0 1 2 15
fi
chmod +x "$command_path"
cd "$base"
exec "$command_path" "$@"
