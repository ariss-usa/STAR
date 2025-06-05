from serial import SerialException
import helper
import re
import platform
from subprocess import Popen, PIPE
import os
import zmq
from threading import Thread
import shutil
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
            try:
                module = helper.import_module_by_platform("winpty", current_platform)
                self.ptyModule = module.PtyProcess
            except Exception as e:
                print("Failed to import pywinpty")
    def checkAPRSUpdates(self):
        direwolf = self.processList[0]
        serialPort = helper.getSerial()

        while direwolf.isalive():
            try:
                line = direwolf.readline()
                print(line)
                match = re.search(r'(\w+)>[^:]+:\s*\[(.*?)\]', line)
                if match:
                    commands = match.group(2).split(",")
                    for cmd in commands:
                        div = cmd.split()
                        try:
                            helper.postToSerialJson([{"power": div[0], "direction": div[1], "time": div[2]}])
                        except (RuntimeError, SerialException) as e:
                            #Log this
                            print("[DEBUG] Error executing command from direwolf - check robot connection")
                            pass
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

    def startAPRSprocesses(self):
        #gets mic input
        if platform.system() == 'Windows' and self.ptyModule:
            if shutil.which("direwolf") is None:
                raise RuntimeError("Direwolf not found in PATH. Please install it or add to PATH")
            try:
                direwolf = self.ptyModule.spawn("direwolf -c ./direwolf-win.conf")
                self.processList.append(direwolf)
            except Exception as e:
                raise RuntimeError(f"Failed to launch Direwolf: {str(e)}")
        #sdr input
        elif(platform.system() == "Linux"):
            self.continueFlag = True

            try:
                shellscript = Popen(["bash", "script.sh"], stdout=PIPE, universal_newlines=True)
                os.set_blocking(shellscript.stdout.fileno(), False)
                self.processList.append(shellscript)
            except Exception as e:
                raise RuntimeError(f"Failed to run SDR script: {str(e)}")
            
        return self.processList

    def stop(self):
        self.continueFlag = False
        for i in self.processList:
            if i.pid is not None:
                i.terminate()
                i.wait()
        self.processList = []
        
        if platform.system() == "Linux":
            os.system("./cleanup.sh")
            
        print("APRS PROCESSESES KILLED")
