#!/bin/bash

cd "$(dirname "$0")"

echo "[INFO] Starting backend..."

./dist/backend/backend &

echo "[INFO] Starting GUI..."

./dist/MissionController/bin/MissionController

