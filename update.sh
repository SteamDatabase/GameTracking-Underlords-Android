#!/bin/bash
set -xeo pipefail
touch .error

# --- Bootstrap ----------
cat manifest_bootstrap.json

APKURL="$(jq -er '.packages.androidarm64.url' manifest_bootstrap.json)"
MVER="$(jq -er '.packages.androidarm64.version' manifest_bootstrap.json)"

# --- APK tracking ----------
IFS=$'\n'

wget -nv -O androidarm64.apk "$APKURL"

# decompile APK
rm -rf android
./tools/bin/jadx -ds android -dr android androidarm64.apk
find android/ -type f > android/filelist.txt

# dump protos

rm -rf ./Protobufs
dotnet ./tools/bin/ProtobufDumper.dll android/lib/arm64-v8a/libserver.so ./Protobufs/
dotnet ./tools/bin/ProtobufDumper.dll android/lib/arm64-v8a/libsteamclient.so ./Protobufs/
git add -f ./Protobufs/*.proto

# strings libraries
set +e
for SOFILE in $(find android -type f -name '*.so'); do

    nm -C -p "${SOFILE}" | grep -Evi "GCC_except_table|google::protobuf" | awk '{$1=""; print $0}' | sort -u > "${SOFILE%.so}.txt"
    strings "${SOFILE}" -n 5 | grep -Evi "protobuf|GCC_except_table|osx-builder\." | c++filt -_ | sort -u > "${SOFILE%.so}_strings.txt"
    git add -f "${SOFILE%.so}.txt" "${SOFILE%.so}_strings.txt"

done
set -eo pipefail

# track android files
find android/com/valvesoftware -type f -name '*.java' -exec git add -f {} +

git add -f android/AndroidManifest.xml android/filelist.txt
find android/res/ -type f -regextype egrep -iregex 'android/res/values.*\.xml' -exec git add -f {} +

# --- Manifest tracking ----------

# update game manifests

grep ManifestPasswordString android/res/values/strings.xml | cut -d'>' -f2 | cut -d'<' -f1 > password.txt
PASSWORD="$(head -n 1 password.txt)"

for PLATFORM in android ios; do

    curl -sSf "https://www.dota2.com/project7manifest/?platform=${PLATFORM}&appid=1046930&version=${MVER}&password=${PASSWORD}" | jq -eMS '.' > manifest_${PLATFORM}.json
    git add -f manifest_${PLATFORM}.json

done

# --- Game tracking ----------

# clean up all previous game files
rm -rf ./game

# grab CDN url
CDNROOT="$(jq -er '.cdnroot' manifest_android.json)"

# download all game files
for FPATH in $(jq -er '.assets | keys[]' manifest_android.json); do

    mkdir -p "$(dirname ${FPATH})"
    wget -nv -O "${FPATH}" "${CDNROOT}${FPATH}"

done

# list and unpack vpks
for VPKPATH in $(find game -type f -name '*_dir.vpk'); do

    VPKNAME="${VPKPATH%.vpk}"
    ./tools/bin/vpk -la "${VPKPATH}" > "${VPKNAME}.txt"
    ./tools/bin/vpk -x "${VPKNAME}" -re '\.(txt|gi|cfg|pem|inf|json|gameevents|lst)$' "${VPKPATH}"

done

# Normalize UTF16 files

for FPATH in $(find game android -type f -name '*.txt' -exec file {} \; | grep -E ": .*(Little-endian UTF-16|data)" | cut -d':' -f1); do

    iconv -f UTF16LE -t UTF8 "${FPATH}" | sed '1s/^\xEF\xBB\xBF//' > "${FPATH}.tmp"
    mv "${FPATH}.tmp" "${FPATH}"

done

# add files for tracking
find game -type f -regextype egrep -iregex '.*\.(txt|gi|cfg|pem|inf|json|gameevents|lst)' -exec git add -f {} +

# stage deleted files
git add -f -u
git reset update.sh

# --- Commit ----------

# create a nice commit
if [ $(git diff --cached | wc -l) -ne 0 ]; then

    git commit -m "v${MVER} | $(git status --porcelain | wc -l) files | $(git status --porcelain | sed '{:q;N;s/\n/, /g;t q}' | sed 's/^ *//g' | cut -c 1-1024)" > /dev/null
    git push > /dev/null 2>&1

fi

rm .error
