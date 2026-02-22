#!/bin/bash

# script to auto decode packet using rtl_fm and Direwolf
sudo modprobe snd-aloop

pkill -o chromium &>/dev/null

sudo killall -9 direwolf &>/dev/null

sudo killall -9 rtl_fm &>/dev/null

sudo killall -9 aplay &>/dev/null

sudo killall -9 qsstv &>/dev/null

sudo killall -9 rtl_tcp &>/dev/null

#sudo killall -9 java &>/dev/null

sudo killall -9 CubicSDR &>/dev/null

sudo killall -9 zenity &>/dev/null

echo


direwolf -r 48000 -c direwolf.conf -t 0 &
sleep 5

value=`aplay -l | grep "Loopback"`
echo "$value" > /dev/null
set -- $value

rtl_fm -M fm -f 144390000 -s 48k | (aplay -D hw:${2:0:1},0,0 -r 48000 -t raw -f S16_LE -c 1)
sleep 5