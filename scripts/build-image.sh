#!/usr/bin/env bash
set -e

./gradlew clean bootBuildImage --imageName=psycwall:latest

