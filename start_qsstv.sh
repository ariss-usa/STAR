#!/bin/bash
# script to decode SSTV using rtl_fm and QSSTV from the C

echo "Script to decode SSTV from the CubeSatSim for ARISS Radio Pi"

echo

sudo systemctl stop openwebrx

sudo modprobe snd-aloop

sudo killall -9 qsstv &>/dev/null

sudo systemctl stop rtl_tcp

pkill -o chromium &>/dev/null

sudo killall -9 rtl_tcp &>/dev/null

sudo killall -9 java &>/dev/null

sudo killall -9 rtl_fm &>/dev/null

sudo killall -9 CubicSDR &>/dev/null

./qsstv &

sleep 5

LOOPBACK_CARD=$(aplay -l | grep "Loopback" | grep -oP 'card \K\d+' | head -1)
echo "Using Loopback card: $LOOPBACK_CARD"

#rtl_fm -M fm -f 434.9M -s 48k | aplay -D hw:2,0,0 -r 48000 -t raw -f S16_LE -c 1 &
rtl_fm -M fm -f 434.9M -s 48k | aplay -D hw:${LOOPBACK_CARD},0,0 -r 48000 -t raw -f S16_LE -c 1

$SHELL