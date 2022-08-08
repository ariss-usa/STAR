sudo modprobe snd-aloop
sudo killall -9 rtl_fm &>/dev/null
sudo killall -9 direwolf &>/dev/null

#Get frequeny from user depending on satellite use
echo
echo "Enter the frequency in kiloHertz"
echo
read -r frequency

frequency=$frequency"000"

echo -e "Decoding AX.25 packets on $frequency kHz"

#run direwolf file

direwolf -r 48000 -c /home/pi/CubeSatSim/groundstation/direwolf/direwolf.conf -t 0 > x.txt &

#loop it to microphone input for direwolf and then get packets at particular frequency, and play it 
sleep 5
value=`aplay -l | grep "Loopback"`
echo "$value" > /dev/null
set -- $value
rtl_fm -M fm -f $frequency -s 48k | tee >(aplay -D hw:${2:0:1},0,0 -r 48000 -t raw -f S16_LE -c 1) | aplay -D hw:0,0 -r 48000 -t raw -f S16_LE -c 1
sleep 5
