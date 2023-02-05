import helper
import os

path_to_file = "important.txt"
while (os.path.exists(path_to_file) == False):
    pass

#TODO: Change to get call signs instead of mcID
with open('important.txt') as f:
    mcID = f.readline().strip()

continueFlag = True
def checkAPRSUpdates():
    global continueFlag
    while True:
        if(helper.getSerial() is None):
            continue
        if(continueFlag == False):
            break
        str = "To " + mcID
        with open('x.txt', 'r+') as f:
            if str in f.read():
                my_file = open("x.txt")
                lines = my_file.readlines()
                for line in lines:
                    if str in line:
                        index = line.find(str) + 9
                        endIndex = line.find("<0x0a>")
                        serialPort = helper.getSerial()
                        helper.postToSerial(serialPort, [line[index:endIndex]])
                        break
                f.truncate(0)
def stop():
    global continueFlag
    continueFlag = False