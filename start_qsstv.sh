#!/bin/bash
# script to decode SSTV using rtl_fm and QSSTV from the CubeSatSim Camera Module

echo "Script to decode SSTV from the CubeSatSim for ARISS Radio Pi"

echo

# Prompt the user to select a receive frequency via Zenity dialog
FREQ=$(zenity --list \
    --title="Select SSTV Frequency" \
    --text="Select the XRP Camera Module frequency to receive:" \
    --column="Participant" --column="Frequency (MHz)" \
    --width=380 --height=420 \
    "Default" "434.90" \
    "1"       "434.91" \
    "2"       "434.92" \
    "3"       "434.93" \
    "4"       "434.94" \
    "5"       "434.95" \
    "6"       "434.89" \
    "7"       "434.88" \
    "8"       "434.87" \
    "9"       "434.86" \
    "10"      "434.85" \
    --print-column=2 2>/dev/null)

# If the user closed/cancelled the dialog, exit without starting QSSTV
if [ -z "$FREQ" ]; then
    echo "No frequency selected. Exiting."
    exit 1
fi

echo "Selected frequency: ${FREQ} MHz"

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
rtl_fm -M fm -f ${FREQ}M -s 48k | aplay -D hw:${LOOPBACK_CARD},0,0 -r 48000 -t raw -f S16_LE -c 1

$SHELL