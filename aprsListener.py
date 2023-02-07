import subprocess
import helper
import os
import threading

path_to_file = "callsign.txt"
while (os.path.exists(path_to_file) == False):
    pass

with open('important.txt') as f:
    callsign = f.readline().strip()

continueFlag = True

def checkAPRSUpdates():
    global continueFlag

    lock = threading.Lock()
    while True:
        if(helper.getSerial() is None):
            continue
        if(continueFlag == False):
            print("END TASK")
            break
        str = "To " + callsign
        with lock:
            with open('output.txt', 'r+') as f:
                if str in f.read():
                    my_file = open("output.txt")
                    lines = my_file.readlines()
                    for line in lines:
                        if str in line:
                            index = line.find(str) + 9
                            endIndex = line.find("<0x0a>")
                            serialPort = helper.getSerial()
                            helper.postToSerial(serialPort, [line[index:endIndex]])
                            break
                    f.truncate(0)
def startAPRSprocesses():
    rtl_fm = subprocess.Popen(["rtl_fm", "-f", "144.390M", "-s", "48000", "-g", "20"],
                    stdout=subprocess.PIPE)
    direwolf = subprocess.Popen(["direwolf", "-c", "direwolf.conf", "-r", "48000", "-D", "1"],
                    stdin=rtl_fm.stdout, stdout=subprocess.PIPE)
    with open("output.txt", "w") as f:
        decode_aprs = subprocess.Popen(["decode_aprs"], stdin=direwolf.stdout, stdout=f)

    return [rtl_fm, direwolf, decode_aprs]
    
def stop(processList):
    global continueFlag
    continueFlag = False
    for i in processList:
        i.kill()
    print("APRS PROCESSES KILLED")
    
