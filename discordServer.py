import asyncio
import struct
from threading import Thread
import traceback
import zmq
import discord
import os
import serial
from dotenv import load_dotenv
from discord.ext import commands
import serial.tools.list_ports
import re
import time
import numpy as np
import subprocess
import helper
import aprsListener
import multiprocessing

intents = discord.Intents.all()
client = commands.Bot(command_prefix="prefix", intents=intents)

#Environment variables
load_dotenv()
token = os.getenv('DISCORD_TOKEN')
CHANNEL_ID = int(os.getenv('CHANNEL_ID'))
DIR_ID = int(os.getenv('DIR_ID'))

##Request/Response from client
context = zmq.Context()
socket = context.socket(zmq.REP)
socket.bind("tcp://127.0.0.1:5555")

myMC = "TBD"
schoolName = "TBD"
city = "TBD"
state = "TBD"
sdrDonglePresent = "TBD"
#localOrRemote =    "TBD"
online = "Yes"
connected = "No"
doNotDisturb = True
serialPort = serial.Serial()

@client.event
async def on_ready():
    global dirArr
    global messageToID
    global serialPort
    messageToID = {}
    dirArr = []
    btconfigTimer = None
    runOnce = True
    aprsProcesses = None
    async for message in client.get_channel(DIR_ID).history(limit=200):
        dirArr.insert(0, message)
        messageToID[message.content[:5]] = message.id
    
    while not client.is_closed():
        c = ""
        try:
            c = socket.recv(flags=zmq.NOBLOCK)
            c = c.decode('utf-8')
            if "SEND" in c:
                #Update selectedMC here and get command
                #"SEND 12345 F 255 255 10"
                command = client.get_command("SEND")
                spl = c.split(" Selected MCid: ")
                getCommand = spl[0][5:]
                getSelectedID = spl[1][:]
                await command(getCommand, getSelectedID)
            elif "Local" in c:
                spl = c.split("Local ")
                #var = spl[1]
                words = spl[1].split()
                grouped_words = [' '.join(words[i: i + 3]) for i in range(0, len(words), 3)]
                socket.send_string("ACK")
                helper.postToSerial(serialPort, grouped_words)
                #serialPort.write(var.encode())
            elif "sendToDIR" in c:
                command = client.get_command("sendToDIR")
                socket.send_string("REC")
                await command(0)
            elif "editOnline" in c:
                command = client.get_command("edit_message_online")
                var = c.split("ChangeTo: ")[1]
                socket.send_string("ACK")
                await command(var, messageToID[myMC])
            elif "editConnect" in c:
                command = client.get_command("edit_message_connected")
                await command("Yes", dirArr[-1].id)
            elif "Check file" in c:
                command = client.get_command("checkFile")
                spl = c.split("Check file ")
                socket.send_string("ACK")
                await command(spl[1])
            elif "getDIRList" in c:
                contentList = getContent(dirArr)
                socket.send_string(';'.join(contentList))
            elif "Pair" in c:
                connectOrDisc = c.split(" ")
                portSuccess = True
                if connectOrDisc[1] == "connect":
                    try:
                        portString = connectOrDisc[2]
                        serialPort = serial.Serial(port=portString, baudrate=115200, bytesize=8, timeout=5, stopbits=serial.STOPBITS_ONE)
                        helper.setSerial(serialPort)
                        btconfigTimer = time.time()
                    except serial.SerialException:
                        portSuccess = False
                else:
                    helper.setSerial(None)
                    serialPort.close()
                if portSuccess:
                    socket.send_string("pass")
                else:
                    socket.send_string("fail")
            elif "getCOMList" in c:
                ports = serial.tools.list_ports.comports()
                p = []
                for port in sorted(ports):
                    p.append("{}".format(port))
                socket.send_string(';'.join(p))
            elif "recAPRS" in c:
                #process = subprocess.Popen("rtl_fm -f 144.390M -s 48000 -g 20 | direwolf -c direwolf.conf -r 48000 -D 1 - | decode_aprs > .\output.txt", shell=True)
                socket.send_string("ACK")
                aprsProcesses = aprsListener.startAPRSprocesses()
                thread = Thread(target=aprsListener.checkAPRSUpdates)
                thread.start()
            elif "stopReceivingAPRS" in c:
                socket.send_string("ACK")
                aprsListener.stop(aprsProcesses)
                #process.terminate()
            elif "END" in c:
                if(myMC != "TBD"):
                    closeConnection = client.get_command("edit_message_connected")
                    closeActivity = client.get_command("edit_message_online")
                    await closeConnection("No", messageToID[myMC])
                    await closeActivity("No", messageToID[myMC])
                socket.send_string("ACK")
                context.destroy()
                await client.close()

            if(runOnce and btconfigTimer != None and time.time() - btconfigTimer > 1):
                serialPort.write(bytearray([255, 85, 7, 0, 2, 5, 0, 0, 0, 0]))
                runOnce = False
                
        except zmq.ZMQError as e:
            if e.errno == zmq.EAGAIN or e.errno == zmq.Again:
                await asyncio.sleep(1)
            else:
                traceback.print_exc()

@client.command()
async def SEND(comm, selectedID):
    #Send request to general channel
    channel = client.get_channel(CHANNEL_ID)
    content = f"{selectedID} connection request from {myMC}".strip() + f", command: {comm}"
    socket.send_string("ACK")
    await channel.send(content)
  
@client.command()
async def sendToDIR(x):
    channel = client.get_channel(DIR_ID)
    content = f"{myMC}\nSchool Name: {schoolName}\nSDR Dongle: {sdrDonglePresent}\nState: {state}\nCity: {city}\nOnline: {online}\nConnected: {connected}"
    await channel.send(content)

@client.command()
async def edit_message_online(onlineStatus, messageID):
    #When user closes application/starts the application
    #onlineStatus is a string --> Yes/No
    global doNotDisturb
    if onlineStatus == "Yes":
        doNotDisturb = False
    else:
        doNotDisturb = True
    channel = client.get_channel(DIR_ID)
    message = await channel.fetch_message(messageID)
    await message.edit(content = f"{myMC}\nSchool Name: {schoolName}\nSDR Dongle: {sdrDonglePresent}\nState: {state}\nCity: {city}\nOnline: {onlineStatus}\nConnected: {connected}")

@client.command()
async def edit_message_connected(connectedStatus, messageID):
    #When connection between two MC's ends/disconnects or connects
    #connectedStatus is a string --> yes/no
    channel = client.get_channel(DIR_ID)
    message = await channel.fetch_message(messageID)
    await message.edit(content = f"{myMC}\nSchool Name: {schoolName}\nSDR Dongle: {sdrDonglePresent}\nState: {state}\nCity: {city}\nOnline: {online}\nConnected: {connectedStatus}")

@client.command()
async def checkFile(editMessage):
    global myMC, schoolName, city, state, sdrDonglePresent#, localOrRemote
    with open("important.txt", "r") as f:
        myMC = f.readline().strip()
        schoolName = f.readline().strip()       #
        city = f.readline().strip()             #"Houston"
        state = f.readline().strip()            #"TX"
        sdrDonglePresent = f.readline().strip() #"No"
        #localOrRemote =  f.readline().strip()   #"Internet"
    channel = client.get_channel(DIR_ID)

    if(editMessage == "true"):
        message = await channel.fetch_message(messageToID[myMC])
        await message.edit(content = f"{myMC}\nSchool Name: {schoolName}\nSDR Dongle: {sdrDonglePresent}\nState: {state}\nCity: {city}\nOnline: {online}\nConnected: {connected}")
    else:
        command = client.get_command("sendToDIR")
        await command()

@client.event
async def on_message(message):
    #listening
    #print(message.content)
    global serialPort
    global doNotDisturb
    newContext = zmq.Context()
    socket1 = newContext.socket(zmq.REQ)
    socket1.bind("tcp://127.0.0.1:5556")

    recMCid = message.content[0:5]
    if myMC == recMCid and message.channel.id == CHANNEL_ID and not doNotDisturb:
        sendString = f"New Command: {message.content}"
        socket1.send_string(sendString)
        socket1.recv()

        split = re.split('command: |\[|\]|\[]', message.content)      
        if(len(split) == 2):
            serialPort.write(split[1].encode())
        else:
            l = []
            amtOfCommands = len(split[2].split(", "))
            powerArr = split[2].split(", ")
            directionArr = split[4].split(", ")
            timeArr = split[6].split(", ")
            for i in range(0, amtOfCommands):
                l.append(f"{powerArr[i]} {directionArr[i]} {timeArr[i]}")
            
            helper.postToSerial(serialPort, l)
            
    elif message.channel.id == DIR_ID:
        #Keep a list of the messages
        global dirArr
        dirArr.append(message)
        messageToID[message.content[:5]] = message.id
        sendString = f"New Robot: {message.content}"
        socket1.send_string(sendString)
        socket1.recv()

    newContext.destroy()
    await client.process_commands(message)

@client.event
async def on_raw_message_edit(payload):
    #Called when message is edited
    #Update the directory list
    global myMC

    message = payload.cached_message
    if not message and DIR_ID == payload.channel_id:
        channel = client.get_channel(DIR_ID)
        x = payload.message_id
        message = await channel.fetch_message(payload.message_id)
    
    if(message.content[0:5] == myMC):
        return

    var = dirArr.index(message)

    newEditContext = zmq.Context()
    socket2 = newEditContext.socket(zmq.REQ)
    socket2.bind("tcp://127.0.0.1:5557")

    socket2.send_string(f"{message.content}\nIndex: {var}")
    socket2.recv()

    newEditContext.destroy()
 
def getContent(dirArr):
    arr = []
    for i in dirArr:
        arr.append(i.content)
    return arr

def postToSerial(commandList):
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
client.run(token)