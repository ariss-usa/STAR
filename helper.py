import time
import importlib

serialPort = None
def postToSerial(serialPort, commandList):
    for i in range(0, len(commandList)):
        splitCommands = commandList[i].split(" ")
        #splitCommands[0] = power, [1] = direction, [2] = time
        rd = 0
        rs = 0
        ld = 0
        ls = 0
        timeOfOperation = float(splitCommands[2])
        if(splitCommands[1] == "forward"):
            ld = 255
            rs = int(float(splitCommands[0]))
            ls = 256-rs
        elif(splitCommands[1] == "backward"):
            rd = 255
            ls = int(float(splitCommands[0]))
            rs = 256-ls
        elif(splitCommands[1] == "right"):
            ld = 255
            rd = 255
            ls = 256 - int(float(splitCommands[0]))
            rs = ls
        elif(splitCommands[1] == "left"):
            rd = 0
            ld = 0
            ls = int(float(splitCommands[0]))
            rs = ls
        if(splitCommands[1] != "delay"):
            serialPort.write(bytearray([255, 85, 7, 0, 2, 5, ls, ld, rs, rd]))
        time.sleep(timeOfOperation)
        serialPort.write(bytearray([255, 85, 7, 0, 2, 5, 0, 0, 0, 0]))

def postToSerialJson(commandList):
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
            serialPort.write(bytearray([255, 85, 7, 0, 2, 5, ls, ld, rs, rd]))
        time.sleep(op_time)
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