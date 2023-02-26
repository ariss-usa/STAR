import subprocess
import helper
import os
import time

class APRSUpdater:
    def __init__(self):
        self.path_to_file = "callsign.txt"
        self.continueFlag = True
        
    def wait_for_file(self):
        while not os.path.exists(self.path_to_file):
            time.sleep(1)
        with open(self.path_to_file) as f:
            self.mycall = f.readline().strip()

    def checkAPRSUpdates(self):
        while True:
            if(helper.getSerial() is None):
                continue
            if(self.continueFlag == False):
                print("END TASK")
                break
            #TODO: account for if callsign is ALLCALL
            str = "To " + self.mycall
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

    def startAPRSprocesses(self):
        rtl_fm = subprocess.Popen(["rtl_fm", "-f", "144.390M", "-s", "48000", "-g", "20"],
                        stdout=subprocess.PIPE)
        direwolf = subprocess.Popen(["direwolf", "-c", "direwolf.conf", "-r", "48000", "-D", "1"],
                        stdin=rtl_fm.stdout, stdout=subprocess.PIPE)
        with open("output.txt", "a") as f:
            decode_aprs = subprocess.Popen(["decode_aprs"], stdin=direwolf.stdout, stdout=f)

        return [rtl_fm, direwolf, decode_aprs]

    def stop(self, processList):
        self.continueFlag = False
        for i in processList:
            i.kill()
        print("APRS PROCESSESES KILLED")
