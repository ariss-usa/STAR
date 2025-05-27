import asyncio
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
import helper
from playsound import playsound
from aprsListener import APRSUpdater
import platform
import subprocess
from pydub import AudioSegment
from pydub.playback import play

if platform.system() == "Windows":
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    
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
errorSocket = context.socket(zmq.REQ)
errorSocket.bind("tcp://127.0.0.1:5558")

myMC = "TBD"
schoolName = "TBD"
city = "TBD"
state = "TBD"
sdrDonglePresent = "TBD"
online = "Yes"
connected = "No"
doNotDisturb = True
serialPort = serial.Serial()

thread = None

@client.event
async def on_ready():
    global dirArr
    global messageToID
    global serialPort
    messageToID = {}
    dirArr = []
    APRSupdater = APRSUpdater()
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
                words = spl[1].split()
                grouped_words = [' '.join(words[i: i + 3]) for i in range(0, len(words), 3)]
                socket.send_string("ACK")
                helper.postToSerial(serialPort, grouped_words)
            elif "sendToDIR" in c:
                command = client.get_command("sendToDIR")
                socket.send_string("REC")
                await command()
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
                        #stop sequence for the mBot
                        serialPort.write(bytearray([255, 85, 7, 0, 2, 5, 0, 0, 0, 0]))
                    except serial.SerialException:
                        portSuccess = False
                else:
                    helper.setSerial(None)
                    serialPort = serial.Serial()
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
                if(platform.system() == "Linux" and not check_rtlsdr()):
                    socket.send_string("no rtl-sdr")
                    continue
                
                processes = APRSupdater.startAPRSprocesses()
                
                if(platform.system() == "Linux"):
                    thread = Thread(target=APRSupdater.checkAPRSUpdates_RTLSDR)
                else:
                    thread = Thread(target=APRSupdater.checkAPRSUpdates)

                thread.start()
                
                socket.send_string("ACK")

                """
                q = Queue()
                t = Thread(target=enqueue_output, args=(rtl_fm.stderr, q))
                t.daemon = True
                t.start()
                
                try:  
                    line = q.get(timeout=.1)
                except Empty:
                    thread = Thread(target=APRSupdater.checkAPRSUpdates)
                    thread.start()
                    print("RUNNING")
                    socket.send_string("ACK")
                else:
                    decoded_line = line.decode()
                    if "No supported devices found." in decoded_line:
                        socket.send_string("rtl_fm stopped")
                        APRSupdater.stop()
                        continue
                """
            
            elif "Transmit APRS" in c:
                mycallsign = c.split()[2]
                command = c.split()[3] + " " + c.split()[4] + " " + c.split()[5]
                wantedCall = c.split()[6]
                if platform.system() == "Windows":
                    oscommand = f"echo {mycallsign}^^^>WORLD: To {wantedCall} {command} | gen_packets -a 25 -o x.wav -"
                    os.system(oscommand)
                    playsound('./x.wav')
                elif platform.system() == "Linux":
                    #oscommand = f'echo "{mycallsign}>WORLD: To {wantedCall} {command}" | tee >(gen_packets -a 25 -o x.wav -) > /dev/null'
                    oscommand = f"echo -n '{mycallsign}>WORLD: To {wantedCall} {command}' | gen_packets -a 25 -o x.wav -"
                    os.system(oscommand)
                    sound = AudioSegment.from_wav("./x.wav")
                    play(sound)
                socket.send_string("ACK")
            elif "stopReceivingAPRS" in c:
                socket.send_string("ACK")
                APRSupdater.stop()
            elif "END" in c:
                if(myMC != "TBD"):
                    closeConnection = client.get_command("edit_message_connected")
                    closeActivity = client.get_command("edit_message_online")
                    await closeConnection("No", messageToID[myMC])
                    await closeActivity("No", messageToID[myMC])
                    await asyncio.sleep(1)
                
                socket.send_string("ACK")
                context.destroy()
                await client.close()
                
        except zmq.ZMQError as e:
            if e.errno == zmq.EAGAIN or e.errno == zmq.Again:
                await asyncio.sleep(1)
            else:
                traceback.print_exc()

def enqueue_output(errstream, queue):
    for line in iter(errstream.readline, b''):
        queue.put(line)
    errstream.close()

def check_rtlsdr():
    try:
        output = subprocess.check_output(["lsusb"]).decode("utf-8")
        if "RTL" in output:
            print("rtl-sdr found")
            return True
        else:
            print("rtl-sdr not found")
            return False
    except subprocess.CalledProcessError as e:
        print("Error executing lsusb:", e)

@client.command()
async def SEND(comm, selectedID):
    #Send request to general channel
    channel = client.get_channel(CHANNEL_ID)
    content = f"{selectedID} connection request from {myMC}".strip() + f", command: {comm}"
    socket.send_string("ACK")
    await channel.send(content)
  
@client.command()
async def sendToDIR():
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
        schoolName = f.readline().strip()       
        city = f.readline().strip()             
        state = f.readline().strip()            
        sdrDonglePresent = f.readline().strip()
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
            helper.postToSerial(serialPort, [split[1]])
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

client.run(token)