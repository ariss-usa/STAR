import subprocess
import helper
import os
import msvcrt

path_to_file = "callsign.txt"
while (os.path.exists(path_to_file) == False):
    pass

with open('callsign.txt') as f:
    mycall = f.readline().strip()

continueFlag = True

def checkAPRSUpdates():
    global continueFlag
    while True:
        if(helper.getSerial() is None):
            continue
        if(continueFlag == False):
            print("END TASK")
            break
        #TODO: account for if callsign is ALLCALL
        str = "To " + mycall
        with open('output.txt', 'r') as f:
            contents = f.read()
            print("Run")
            if str in contents:
                index = contents.find(str)
                endIndex = contents.find("<0x0a>")
                serialPort = helper.getSerial()
                l = contents[index:endIndex].split()
                command = f"{l[2]} {l[3]} {l[4]}"
                helper.postToSerial(serialPort, [command])
                break
"""                 for line in contentList:
                    if str in line:
                        index = line.find(str) + 9
                        endIndex = line.find("<0x0a>")
                        serialPort = helper.getSerial()
                        helper.postToSerial(serialPort, [line[index:endIndex]])
                        break """
                #my_file.truncate(0)

def startAPRSprocesses():
    rtl_fm = subprocess.Popen(["rtl_fm", "-f", "144.390M", "-s", "48000", "-g", "20"],
                    stdout=subprocess.PIPE)
    direwolf = subprocess.Popen(["direwolf", "-c", "direwolf.conf", "-r", "48000", "-D", "1"],
                    stdin=rtl_fm.stdout, stdout=subprocess.PIPE)
    with open("output.txt", "a") as f:
        decode_aprs = subprocess.Popen(["decode_aprs"], stdin=direwolf.stdout, stdout=f)

    return [rtl_fm, direwolf, decode_aprs]
    
def stop(processList):
    global continueFlag
    continueFlag = False
    for i in processList:
        i.kill()
    print("APRS PROCESSESES KILLED")
