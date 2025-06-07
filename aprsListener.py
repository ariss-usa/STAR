from serial import SerialException
from robot_link import RobotLink
import re
import platform
from subprocess import Popen, PIPE
import os
import shutil
class APRSUpdater:
    def __init__(self, link):
        self.continueFlag = True
        self.processList = []
        current_platform = platform.system()
        self.ptyModule = None
        self.link = link
        if current_platform == "Windows":
            try:
                module = RobotLink.import_module_by_platform("winpty", current_platform)
                self.ptyModule = module.PtyProcess
            except Exception as e:
                print("Failed to import pywinpty")
    
    def postCommands(self, line):
        match = re.search(r'(\w+)>[^:]+:\s*\[(.*?)\]', line)
        if match:
            commands = match.group(2).split(",")
            json_cmds = []

            if self.checkFormat(commands):
                for cmd in commands:
                    div = cmd.split()
                    json_cmds.append({"power":div[0], "direction":div[1], "time":div[2]})

                try:
                    self.link.postToSerialJson(json_cmds)
                except (RuntimeError, SerialException):
                    #Log this
                    print("[DEBUG] Error executing command from direwolf - check robot connection")
                    pass

    def checkAPRSUpdates(self):
        direwolf = self.processList[0]

        while direwolf.isalive():
            try:
                line = direwolf.readline()
                print(line)
                self.postCommands(line)
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
                if (not (self.isFloat(linearr[0]) and (linearr[1] == "left" or linearr[1] == "right" or linearr[1] == "forward" or linearr[1] == "backward" or linearr[1] == "delay") and self.isFloat(linearr[2]) and
                    float(linearr[0]) >= 0 and float(linearr[0]) <= 255 and float(linearr[2]) >= 0 and float(linearr[2]) <= 120)
                    ):
                        return False
        return True

    def checkAPRSUpdates_Linux(self):
        direwolf = self.processList[0]
        while self.continueFlag:
            line = direwolf.stdout.readline()
            if line != '':
                self.postCommands(line)

    def startAPRSprocesses(self):
        #gets mic input
        if platform.system() == 'Windows':
            try:
                direwolf = self.ptyModule.spawn(".\\direwolf.exe -c .\\direwolf-win.conf")
                self.processList.append(direwolf)
            except Exception as e:
                raise RuntimeError(f"Failed to launch Direwolf")
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
            
        print("[DEBUG] APRS processes killed")
