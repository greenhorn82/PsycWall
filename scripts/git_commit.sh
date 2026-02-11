#!/usr/bin/env bash

set -e

echo "📦 Git Release Script"

# --- Version einlesen ---
if [ -z "$1" ]; then
  read -p "Version (z.B. 1.2.0): " VERSION
else
  VERSION=$1
fi

# SemVer grob prüfen
if [[ ! $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "❌ Ungültige SemVer Version (MAJOR.MINOR.PATCH)"
  exit 1
fi

# --- Commit Message ---
read -p "Commit Message: " MESSAGE

if [ -z "$MESSAGE" ]; then
  MESSAGE="Release $VERSION"
fi

# --- Release Tag? ---
read -p "Auch 'release' Tag setzen? (y/N): " SET_RELEASE

# --- Änderungen committen ---
echo "📄 Änderungen werden committed..."
git add .

# Commit nur wenn nötig
if ! git diff --cached --quiet; then
  git commit -m "$MESSAGE"
else
  echo "ℹ️ Keine Änderungen zum Committen"
fi

# --- SemVer Tag ---
echo "🏷️ Setze Tag $VERSION"
git tag -a "$VERSION" -m "Release $VERSION"

# --- Optional Release Tag ---
if [[ "$SET_RELEASE" =~ ^[Yy]$ ]]; then
  echo "🏷️ Setze zusätzlich 'release' Tag"

  # Alten release Tag löschen (lokal + remote)
  if git rev-parse release >/dev/null 2>&1; then
    git tag -d release
    git push origin :refs/tags/release
  fi

  git tag -a "release" -m "Release $VERSION"
fi

# --- Build lokal docker ---
#./gradlew clean bootBuildImage --imageName=psycwall:$VERSION
# --- Push ---
echo "🚀 Push zu origin..."
git push origin main --tags

echo "✅ Release $VERSION erstellt!"