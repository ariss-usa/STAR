#!/bin/bash

# This creates a "Virtual Cable" named 'aprs_bridge'
if ! pactl list sinks short | grep -q "aprs_bridge"; then
    pactl load-module module-null-sink sink_name=aprs_bridge sink_properties=device.description="APRS_Virtual_Cable"
fi

direwolf -r 48000 -c direwolf.conf -t 0 &
sleep 2

rtl_fm -M fm -f 144390000 -s 48k | pacat --playback --device=aprs_bridge --format=s16le --channels=1 --rate=48000