import subprocess
import helper
import re
class APRSUpdater:
    def __init__(self):
        self.path_to_file = "callsign.txt"
        self.continueFlag = True
        self.processList = []
        
    def checkAPRSUpdates(self):
        with open(self.path_to_file) as f:
            self.mycall = f.readline().strip()

        decode_aprs = self.processList[2]
        serialPort = helper.getSerial()
        while True:
            str = "To " + self.mycall + " "
            for line in decode_aprs.stdout:
                currline = line.decode().rstrip()
                pattern = re.compile(r'{}(.+?)<0x0a>'.format(re.escape(str)))
                match = pattern.search(currline)
                if match:
                    contents = match.group(1).split()
                    command = f"{contents[0]} {contents[1]} {contents[2]}"
                    helper.postToSerial(serialPort, [command])
            


    def startAPRSprocesses(self):
        rtl_fm = subprocess.Popen(["rtl_fm", "-f", "144.390M", "-s", "48000", "-g", "20"],
                        stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        direwolf = subprocess.Popen(["direwolf", "-c", "direwolf.conf", "-r", "48000", "-D", "1"],
                        stdin=rtl_fm.stdout, stdout=subprocess.PIPE)
        #with open("output.txt", "a") as f:
        #    decode_aprs = subprocess.Popen(["decode_aprs"], stdin=direwolf.stdout, stdout=f)
        decode_aprs = subprocess.Popen(["decode_aprs"], stdin=direwolf.stdout, stdout=subprocess.PIPE)
        #decode_aprs = subprocess.Popen(["python", "decode_aprs_sim.py"], stdout=subprocess.PIPE)
        self.processList = [rtl_fm, direwolf, decode_aprs]
        return self.processList

    def stop(self):
        self.continueFlag = False
        for i in self.processList:
            i.kill()
        print("APRS PROCESSESES KILLED")
