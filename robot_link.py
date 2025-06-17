import time
import importlib

class RobotLink:
    def __init__(self, socket):
        self.serialPort = None
        self.socket = socket

    def postToSerialJson(self, commandList):
        if self.serialPort is None or not self.serialPort.isOpen():
            raise RuntimeError("Robot is not properly connected")

        self.socket.send_json({"type": "command", "commands": commandList})
        for i in range(0, len(commandList)):
            command = commandList[i]
            #splitCommands[0] = power, [1] = direction, [2] = time
            rd = rs = ld = ls = 0
            power = int(float(command["power"]))
            direction = command["direction"]
            op_time = float(command["time"])
            
            if(direction == "forward"):
                ld = 255
                rs = power
                ls = 256-rs
            elif(direction == "backward"):
                rd = 255
                ls = power
                rs = 256-ls
            elif(direction == "right"):
                ld = 255
                rd = 255
                ls = 256 - power
                rs = ls
            elif(direction == "left"):
                rd = 0
                ld = 0
                ls = power
                rs = ls
            if(direction != "delay"):
                self.serialPort.write(bytearray([255, 85, 7, 0, 2, 5, ls, ld, rs, rd]))
            time.sleep(op_time)
            self.serialPort.write(bytearray([255, 85, 7, 0, 2, 5, 0, 0, 0, 0]))

    def setSerial(self, ser):
        self.serialPort = ser
    def getSerial(self):
        return self.serialPort

    def closeSerial(self):
        if self.serialPort:
            self.serialPort.close()
            self.serialPort = None
    
    @staticmethod
    def import_module_by_platform(module_name, platform_name):
        try:
            module = importlib.import_module(module_name)
            print(f"Module '{module_name}' imported successfully on {platform_name}.")
            return module
            # You can now use the imported module in your code.
            # For example: module.some_function()
        except ImportError:
            print(f"Failed to import module '{module_name}' on {platform_name}.")