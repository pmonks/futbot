#!/usr/bin/env bash
set -e
set -o pipefail

MAJOR_MINOR="1.0"
PLACEHOLDER_VERSION="${MAJOR_MINOR}.YYYYMMDD"
NEW_VERSION="${MAJOR_MINOR}.$(date +%Y%m%d)"

echo "▶️ Releasing futbot version ${NEW_VERSION}..."

if [ ! -x "$(command -v hub)" ]; then
  echo "🛑 Unable to find 'hub' executable - is it installed?"
  exit -1
fi

if [ ! -x "$(command -v xmlstarlet)" ]; then
  echo "🛑 Unable to find 'xmlstarlet' executable - is it installed?"
  exit -1
fi

if [ "$(git branch --show-current)" != "dev" ]; then
  echo "🛑 Not currently on 'dev' branch - please switch and try again."
  exit -1
fi

if [ -n "$(git status -s --untracked-files=no)" ]; then
  echo "🛑 Uncommitted changes found. Please commit or move those changes out of the way and try again."
  exit -1
fi

echo "ℹ️ Updating local..."
git fetch origin main:main
git merge main
git pull

echo "❔ Press ENTER if update was clean, Ctrl+C if not..."
read

echo "ℹ️ Updating version in pom.xml..."
xmlstarlet ed --inplace -N pom='http://maven.apache.org/POM/4.0.0' -u '/pom:project/pom:version' -v ${NEW_VERSION} pom.xml

echo "ℹ️ Committing and pushing changes..."
git commit -m ":gem: Release ${NEW_VERSION}" pom.xml
git push

echo "ℹ️ Creating pull request..."
git pull-request --browse -f -m "Release ${NEW_VERSION}" -h dev -b main

echo "⏹ Done."
