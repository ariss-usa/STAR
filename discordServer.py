import asyncio
import traceback
import zmq
import discord
import os
from dotenv import load_dotenv
from discord.ext import commands

intents = discord.Intents.default()
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

#config variables
#Get from GUI
"""
safeToOpenFile = socket.recv().decode("utf-8")
socket.send_string("Received")
with open("important.txt", "r") as f:
    myMC = f.readline().strip()
    schoolName = f.readline().strip()       #"Harmony school of Advancement"
    city = f.readline().strip()             #"Houston"
    state = f.readline().strip()            #"TX"
    sdrDonglePresent = f.readline().strip() #"No"
    localOrRemote =  f.readline().strip()  #"Internet"
"""

myMC = "TBD"
schoolName = "TBD"
city = "TBD"
state = "TBD"
sdrDonglePresent = "TBD"
#localOrRemote =    "TBD"
online = "Yes"
connected = "No"

@client.event
async def on_ready():
    global dirArr
    global messageToID
    messageToID = {}
    dirArr = []
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
            
        except zmq.ZMQError as e:
            if e.errno == zmq.EAGAIN:
                await asyncio.sleep(1)
                pass
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
async def sendToDIR():
    channel = client.get_channel(DIR_ID)
    content = f"{myMC}\nSchool Name: {schoolName}\nSDR Dongle: {sdrDonglePresent}\nState: {state}\nCity: {city}\nOnline: {online}\nConnected: {connected}"
    await channel.send(content)

@client.command()
async def edit_message_online(onlineStatus, messageID):
    #When user closes application/starts the application
    #onlineStatus is a string --> yes/no
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
        schoolName = f.readline().strip()       #"Harmony school of Advancement"
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
    newContext = zmq.Context()
    socket1 = newContext.socket(zmq.REQ)
    socket1.bind("tcp://127.0.0.1:5556")

    recMCid = message.content[0:5]
    if myMC == recMCid and message.channel.id == CHANNEL_ID:
        sendString = f"New Command: {message.content}"
        socket1.send_string(sendString)
        socket1.recv()
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
    #if(localOrRemote != "INTERNET"):
    #    return
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