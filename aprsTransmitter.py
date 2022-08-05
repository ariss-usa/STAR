import asyncio
import traceback
import zmq
import os
#from playsound import playsound

context = zmq.Context()
socket = context.socket(zmq.REP)
socket.bind("tcp://127.0.0.1:5554")

async def on_ready():
    while True:
        c = ""
        try:
            c = socket.recv(flags=zmq.NOBLOCK)
            c = c.decode('utf-8')
            if "APRS" in c:
                #Update selectedMC here and get command
                #"SEND 12345 F 255 255 10"
                spl = c.split(" Selected MCid: ")
                getCommand = spl[0][5:]
                getSelectedID = spl[1][:][0:5]
                getFrequency = spl[1][:][17:25]
                await transmit(getCommand, getSelectedID, getFrequency)
            elif "END" in c: 
                socket.send_string("ACK")
                context.destroy()
                break
        except zmq.ZMQError as e:
                if e.errno == zmq.EAGAIN or e.errno == zmq.Again:
                    await asyncio.sleep(1)
                else:
                    traceback.print_exc()

async def transmit(command, id, freq):
    socket.send_string("ACK")
    str = "echo 'WB2OSZ>WORLD:" + command + "' | gen_packets -o x.wav -b 1200 -"
    os.system(str)
    #playsound('x.wav')
    os.system('aplay x.wav')
    os.system("atest x.wav >> x.txt")
    await asyncio.sleep(0.01)

if __name__ == '__main__':
    asyncio.run(on_ready())
