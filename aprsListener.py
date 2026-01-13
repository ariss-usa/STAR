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
        match = re.search(r'([a-zA-Z\d-]+)>[a-zA-Z\d,-]+::([a-zA-Z\d-]+ {0,8}):\[([\da-zA-Z, ]+)\]', line)
        if match:
            commands = match.group(3).split(",")
            json_cmds = []

            import backend
            from backend import RobotType

            if backend.robotType is None:
                print("[DEBUG] Received APRS packet, but no robot is paired. Ignoring.")
                return

            if not self.checkFormat(commands):
                print("[DEBUG] Received incorrect format")
                return

            for cmd in commands:
                div = cmd.split()
                
                if backend.robotType == RobotType.MBOT:
                    if len(div) == 3:
                        json_cmds.append({"power": div[0], "direction": div[1], "time": div[2]})
                
                elif backend.robotType == RobotType.XRP:
                    if len(div) == 2:
                        json_cmds.append({"direction": div[0], "amount": div[1]})

            if json_cmds:
                if backend.robotType == RobotType.MBOT:
                    self.link.postToSerialJson(json_cmds)
                elif backend.robotType == RobotType.XRP:
                    backend.cmdQueue = json_cmds

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
        import backend
        from backend import RobotType
        
        if backend.robotType is None:
            return False

        for command in commands:
            linearr = command.split()
            
            if backend.robotType == RobotType.MBOT:
                if len(linearr) != 3: return False
                if not (self.isFloat(linearr[0]) and 0 <= float(linearr[0]) <= 255): return False
                if linearr[1] not in ["left", "right", "forward", "backward", "delay"]: return False
                if not (self.isFloat(linearr[2]) and 0 <= float(linearr[2]) <= 120): return False

            elif backend.robotType == RobotType.XRP:
                if len(linearr) != 2: return False
                if linearr[0] not in ["f", "b", "l", "r", "a", "d"]: return False
                if not self.isFloat(linearr[1]): return False
                
        return True

    def checkAPRSUpdates_Linux(self):
        direwolf = self.processList[0]
        while self.continueFlag:
            line = direwolf.stdout.readline()
            if line != '':
                print(line)
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
