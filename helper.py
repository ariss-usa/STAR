import time
import serial

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