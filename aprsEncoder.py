import os
from playsound import playsound

with open('important.txt') as f:
    mcID = f.readline().strip()
str = f"echo WB2OSZ^^^>WORLD: To {mcID} 100 forward 2 | gen_packets -a 25 -o x.wav -"
os.system(str)

playsound('./x.wav')
#os.system('aplay x.wav')
os.system("atest x.wav >> x.txt")