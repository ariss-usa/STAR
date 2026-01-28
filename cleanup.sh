#!/bin/bash

# script to cleanup processes
pkill -o chromium &>/dev/null

sudo killall -9 direwolf &>/dev/null

sudo killall -9 rtl_fm &>/dev/null

sudo killall -9 aplay &>/dev/null

sudo killall -9 qsstv &>/dev/null

sudo killall -9 rtl_tcp &>/dev/null

#sudo killall -9 java &>/dev/null

sudo killall -9 CubicSDR &>/dev/null

sudo killall -9 zenity &>/dev/null

sleep 1

for i in {1..10}; do
    if ! pgrep rtl_fm > /dev/null; then
        break
    fi
    sleep 0.5
done

sleep 2

echo "[CLEANUP] All processes killed, device released"