import helper
from winpty import PtyProcess
import re
class APRSUpdater:
    def __init__(self):
        self.path_to_file = "callsign.txt"
        self.continueFlag = True
        self.processList = []
        
    def checkAPRSUpdates(self):
        with open(self.path_to_file) as f:
            self.mycall = f.readline().strip()

        direwolf = self.processList[0]
        serialPort = helper.getSerial()

        while direwolf.isalive():
            line = direwolf.readline()
            print(line)
            clean_str = re.sub(r'\x1b\[.*?[@-~]', '', line)
            clean_str = clean_str.strip()
            pattern = re.compile(r'{}(.+?)<0x0a>'.format(re.escape(str.encode('utf-8').decode('utf-8'))))
            match = pattern.search(clean_str)
            if match:
                contents = match.group(1).split()
                command = f"{contents[0]} {contents[1]} {contents[2]}"
                helper.postToSerial(serialPort, [command])
            

    def startAPRSprocesses(self):
        #gets mic input
        direwolf = PtyProcess.spawn("direwolf -c direwolf.conf")

        #gets RTL-SDR input
        #direwolf = PtyProcess.spawn("rtl_fm -f 144.390M - | direwolf -c direwolf.conf -")
        self.processList = [direwolf]
        return self.processList

    def stop(self):
        self.continueFlag = False
        for i in self.processList:
            i.kill()
        print("APRS PROCESSESES KILLED")
