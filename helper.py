import time
import serial
import importlib

serialPort = None
def postToSerial(serialPort, commandList):
    for i in range(0, len(commandList)):
        """
        response = ""
        serialPort.write(commandList[i].encode())
        while "FIN" not in response:
            response = serialPort.readline().decode('utf-8').strip()
        """
        splitCommands = commandList[i].split(" ")
        #splitCommands[0] = power, [1] = direction, [2] = time
        ld = 255
        rs = int(float(splitCommands[0]))
        timeOfOperation = float(splitCommands[2])
        ls = 256-rs
        rd = 0

        if(splitCommands[1] == "left"):
            ld = 0
            rd = 0
        elif(splitCommands[1] == "right"):
            ld = 255
            rd = 255
        elif(splitCommands[1] == "backward"):
            ld = 0
            rd = 255
            rs, ls = ls, rs
        
        if(splitCommands[1] != "delay"):
            serialPort.write(bytearray([255, 85, 7, 0, 2, 5, ls, ld, rs, rd]))
        time.sleep(timeOfOperation)
        serialPort.write(bytearray([255, 85, 7, 0, 2, 5, 0, 0, 0, 0]))

def setSerial(ser):
    global serialPort
    serialPort = ser
def getSerial():
    global serialPort
    return serialPort

def import_module_by_platform(module_name, platform_name):
    try:
        module = importlib.import_module(module_name)
        print(f"Module '{module_name}' imported successfully on {platform_name}.")
        return module
        # You can now use the imported module in your code.
        # For example: module.some_function()
    except ImportError:
        print(f"Failed to import module '{module_name}' on {platform_name}.")