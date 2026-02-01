#!/bin/bash
echo "[CLEANUP] Stopping PulseAudio streams..."

sudo killall -15 rtl_fm direwolf qsstv pacat 2>/dev/null
sleep 1
sudo killall -9 rtl_fm direwolf qsstv pacat 2>/dev/null

echo "[CLEANUP] Audio pipeline cleared."