import time
import os
import os.path
import traceback
import zmq
import serial

timer = 300
serialPort = serial.Serial()
context = zmq.Context()
socket = context.socket(zmq.REP)
socket.bind("tcp://127.0.0.1:5553")
path_to_file = "important.txt"
while (os.path.exists(path_to_file) == False):
    pass
with open('important.txt') as f:
    mcID = f.readline().strip()
c = ""    
while True:
    try:
        c = socket.recv(flags=zmq.NOBLOCK)
        c = c.decode('utf-8')
        if "Pair" in c:
            connectOrDisc = c.split(" ")
            portSuccess = True
            if connectOrDisc[1] == "connect":
                try:
                    portString = connectOrDisc[2]
                    serialPort = serial.Serial(port=portString, baudrate=115200, bytesize=8, timeout=5, stopbits=serial.STOPBITS_ONE)
                except serial.SerialException:
                    portSuccess = False
            else:
                serialPort.close()
            if portSuccess:
                socket.send_string("pass")
            else:
                socket.send_string("fail")
        elif "END" in c: 
            socket.send_string("ACK")
            context.destroy()
            break
    except zmq.ZMQError as e:
            if e.errno == zmq.EAGAIN or e.errno == zmq.Again:
                pass
            else:
                traceback.print_exc()
    if not serialPort.is_open:
        timer = 300
        continue
    str = "to " + mcID
    with open('x.txt', 'r+') as f:
        if str in f.read():
            timer = 300
            my_file = open("x.txt")
            lines = my_file.readlines()
            for line in lines:
                if str in line:
                    index = line.find(str) + 9
                    endIndex = line.find("<0x0a>")
                    serialPort.write(line[index:endIndex])
                    break
            f.truncate(0)
        else:
            if len(f.readlines()) > 0:
                f.truncate(0)
            time.sleep(1)
            timer -= 1
            if (timer == 0):
                os.system("sudo killall -9 rtl_fm &>/dev/null")
                os.system("sudo killall -9 direwolf &>/dev/null")
                break
