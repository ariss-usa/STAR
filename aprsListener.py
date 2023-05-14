import helper
import re
import platform

class APRSUpdater:
    def __init__(self):
        self.path_to_file = "callsign.txt"
        self.continueFlag = True
        self.processList = []
        current_platform = platform.system()
        self.ptyModule = None
        if current_platform == "Windows":
            module = helper.import_module_by_platform("winpty", current_platform)
            self.ptyModule = module.PtyProcess
        elif current_platform == "Linux":
            self.ptyModule = helper.import_module_by_platform("pty", current_platform)
        else:
            print("Unsupported platform.")

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
        elif(platform.system() == "Linux") and self.ptyModule:
            direwolf = self.ptyModule.spawn(["direwolf", "-c", "./direwolf.conf"])

        #gets RTL-SDR input
        #direwolf = PtyProcess.spawn("rtl_fm -f 144.390M - | direwolf -c direwolf.conf -")
        self.processList = [direwolf]
        return self.processList

    def stop(self):
        self.continueFlag = False
        for i in self.processList:
            if i.pid is not None:
                i.terminate()
        print("APRS PROCESSESES KILLED")
