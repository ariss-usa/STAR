import helper
import re
import platform
from subprocess import Popen, PIPE
from os import system
class APRSUpdater:
    def __init__(self):
        self.continueFlag = True
        self.processList = []
        current_platform = platform.system()
        self.ptyModule = None
        if current_platform == "Windows":
            module = helper.import_module_by_platform("winpty", current_platform)
            self.ptyModule = module.PtyProcess
    def checkAPRSUpdates(self):
        direwolf = self.processList[0]
        serialPort = helper.getSerial()

        while direwolf.isalive():
            try:
                line = direwolf.readline()
                print(line)
                clean_str = re.sub(r'\x1b\[.*?[@-~]', '', line)
                clean_str = clean_str.strip()
                pattern = re.compile(r'To\s(.*?)\s<0x0a>')
                match = pattern.search(clean_str)
                if match:
                    contents = match.group(1).split()
                    command = f"{contents[1]} {contents[2]} {contents[3]}"
                    print(command)
                    helper.postToSerial(serialPort, [command])
            except EOFError:
                print("Direwolf stopped")
                break

    def checkAPRSUpdates_RTLSDR(self):
        direwolf = self.processList[0]
        serialPort = helper.getSerial()
        with direwolf as p:
            for line in p.stdout:
                print(line)
                clean_str = re.sub(r'\x1b\[.*?[@-~]', '', line)
                clean_str = clean_str.strip()
                pattern = re.compile(r'To\s(.*?)\s<0x0a>')
                match = pattern.search(clean_str)
                if match:
                    contents = match.group(1).split()
                    command = f"{contents[1]} {contents[2]} {contents[3]}"
                    print(command)
                    helper.postToSerial(serialPort, [command])

    def startAPRSprocesses(self):
        #gets mic input
        if platform.system() == 'Windows' and self.ptyModule:
            direwolf = self.ptyModule.spawn("direwolf -c .\direwolf.conf")
            self.processList.append(direwolf)
        #sdr input
        elif(platform.system() == "Linux"):
            system("sudo modprobe snd-aloop")
            rtl_sdr = Popen(["rtl_fm", "-f", "144.390M", "-"], stdout=PIPE)
            direwolf = Popen(["direwolf", "-c", "direwolf.conf", "-"], stdin=rtl_sdr.stdout, stdout=PIPE, bufsize=1, universal_newlines=True)
            self.processList.append(direwolf)
            self.processList.append(rtl_sdr)
        return self.processList

    def stop(self):
        self.continueFlag = False
        for i in self.processList:
            if i.pid is not None:
                i.terminate()
        print("APRS PROCESSESES KILLED")
