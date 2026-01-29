#!/bin/bash

echo "[CLEANUP] Starting cleanup process..."

# script to cleanup processes
pkill -o chromium &>/dev/null
sudo killall -9 direwolf &>/dev/null
sudo killall -9 aplay &>/dev/null
sudo killall -9 qsstv &>/dev/null
sudo killall -9 rtl_tcp &>/dev/null
#sudo killall -9 java &>/dev/null
sudo killall -9 CubicSDR &>/dev/null
sudo killall -9 zenity &>/dev/null

echo "[CLEANUP] Killing rtl_fm (first pass)..."
sudo killall -9 rtl_fm &>/dev/null

sleep 1

echo "[CLEANUP] Killing rtl_fm (second pass)..."
sudo killall -9 rtl_fm &>/dev/null

sleep 1

echo "[CLEANUP] Verifying rtl_fm is terminated..."
for i in {1..10}; do
    if ! pgrep rtl_fm > /dev/null; then
        echo "[CLEANUP] rtl_fm confirmed dead"
        break
    fi
    echo "[CLEANUP] WARNING: rtl_fm still running, attempting to kill again..."
    sudo killall -9 rtl_fm &>/dev/null
    sleep 0.5
done

echo "[CLEANUP] Waiting for USB device to release..."
sleep 10
echo "[CLEANUP] All processes killed, device released"