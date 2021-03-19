#!/usr/bin/env bash
set -e
set -o pipefail

# Sanity checking
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

# Script proper starts here
MAJOR_MINOR="1.0"
NEW_VERSION="${MAJOR_MINOR}.$(date +%Y%m%d)"

echo "▶️ Releasing futbot version ${NEW_VERSION}..."

echo "ℹ️ Updating local..."
git fetch origin main:main
git merge main
git pull

echo "❔ Press ENTER if update was clean, Ctrl+C if not..."
read

echo "ℹ️ Updating version in pom.xml..."
xmlstarlet ed --inplace -N pom='http://maven.apache.org/POM/4.0.0' -u '/pom:project/pom:version' -v ${NEW_VERSION} pom.xml
git commit -m ":gem: Release ${NEW_VERSION}" pom.xml ||:    # Ignore status code, in the case that nothing changed (e.g. when more than one release happens in a day)

echo "ℹ️ Tagging release..."
git tag -f -a "v${NEW_VERSION}" -m "Release v${NEW_VERSION}"

echo "ℹ️ Updating deploy info..."
clj -M:git-info-edn
git commit -m ":gem: Release ${NEW_VERSION}" resources/deploy-info.edn  ||:    # Ignore status code, in the case that nothing changed (e.g. when more than one release happens in a day)

echo "ℹ️ Pushing changes..."
git push
git push origin -f --tags

echo "ℹ️ Creating pull request..."
printf -v PR_DESCRIPTION "Summary of changes:\n\n$(git shortlog --no-merges --abbrev-commit main..dev | tail -n +2 | sed 's/^[[:blank:]]*//g' | sed '/^$/d' | sed -e 's/^/* /')"
hub pull-request --browse -f -m "Release ${NEW_VERSION}" -m "${PR_DESCRIPTION}" -h dev -b main

echo "ℹ️ After the PR has been merged, it is highly recommended that you run the following ASAP:"
echo "  1. git fetch origin main:main"
echo "  2. git merge main"
echo "  3. git pull"
echo "  4. git push"

echo "⏹ Done."
