import helper
import re
import platform
from subprocess import Popen, PIPE
import os
import zmq
from threading import Thread
class APRSUpdater:
    def __init__(self):
        self.continueFlag = True
        self.processList = []
        current_platform = platform.system()
        self.ptyModule = None
        self.context = zmq.Context()
        self.socket = self.context.socket(zmq.REQ)
        self.socket.bind("tcp://127.0.0.1:5559")
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

    def isFloat(self, value):
        if value is None:
            return False
        try:
            float(value)
            return True
        except ValueError:
            return False
    def checkFormat(self, commands):
        for command in commands:
            linearr = command.split()             
            if(len(linearr) != 3):
                return False
            else:
                if (not (self.isFloat(linearr[0]) and (linearr[1] == "left" or linearr[1] == "right" or linearr[1] == "forward" or linearr[1] == "backward") and self.isFloat(linearr[2]) and
                    float(linearr[0]) >= 1 and float(linearr[0]) <= 255 and float(linearr[2]) >= 0 and float(linearr[2]) <= 120)
                    ):
                        return False
        return True

    def checkAPRSUpdates_RTLSDR(self):
        direwolf = self.processList[0]
        serialPort = helper.getSerial()
        while self.continueFlag:
            line = direwolf.stdout.readline()
            if line != '':
                print(line)
                line = line.strip()
                #linearr = line.split()
                pattern = r'\[([^]]+)\]'
                match = re.search(pattern, line)
                if match:
                    content = match.group(1)
                    commands = re.split(r',\s*', content)
                    if self.checkFormat(commands):
                        self.socket.send_string(line)
                        self.socket.recv()
                        helper.postToSerial(serialPort, commands)
        """
        with direwolf as p:
            for line in p.stdout:
                print(line)
                line = line.strip()
                #linearr = line.split()
                pattern = r'\[([^]]+)\]'
                match = re.search(pattern, line)
                if match:
                    content = match.group(1)
                    commands = re.split(r',\s*', content)
                    if self.checkFormat(commands):
                        self.socket.send_string(line)
                        self.socket.recv()
                        helper.postToSerial(serialPort, commands)
        """
    def startAPRSprocesses(self):
        #gets mic input
        if platform.system() == 'Windows' and self.ptyModule:
            direwolf = self.ptyModule.spawn("direwolf -c .\direwolf.conf")
            self.processList.append(direwolf)
        #sdr input
        elif(platform.system() == "Linux"):
            self.continueFlag = True
            shellscript = Popen(["bash", "script.sh"], stdout=PIPE, universal_newlines=True)
            os.set_blocking(shellscript.stdout.fileno(), False)
            self.processList.append(shellscript)
        return self.processList

    def stop(self):
        self.continueFlag = False
        for i in self.processList:
            if i.pid is not None:
                i.terminate()
                i.wait()
        self.processList = []
        os.system("./cleanup.sh")
        print("APRS PROCESSESES KILLED")
