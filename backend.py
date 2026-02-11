import sys
from platform import system
if system() == "Windows":
    from winsound import PlaySound
else:
    from playsound3 import playsound as PlaySound
sys.coinit_flags = 0 # type: ignore
import asyncio
import platform
from threading import Thread, Event
import time
import requests
from requests import RequestException, Timeout
from zmq import PUSH, Context
from zmq import REP
from aprsListener import APRSUpdater
from serial import Serial, SerialException
from serial import STOPBITS_ONE
from serial.tools.list_ports import comports
from bleak import *
import os
import websockets
import json
#from rtlsdr import RtlSdr
from DisconnectMonitor import USBDisconnectWatcher
import sys
from robot_link import RobotLink
import shared_state

ACTIVE_ROBOTS_ENDPOINT = "https://star-bvjn.onrender.com/robots/active"
UPDATE_ROBOT_ENDPOINT = "https://star-bvjn.onrender.com/robots/update"
SEND_COMMAND_ENDPOINT = "https://star-bvjn.onrender.com/send_command"
HEALTH_ENDPOINT = "https://star-bvjn.onrender.com/health"
WEBSOCKET_ENDPOINT = "wss://star-bvjn.onrender.com/ws"

#ACTIVE_ROBOTS_ENDPOINT = "http://127.0.0.1:8000/robots/active"
#UPDATE_ROBOT_ENDPOINT = "http://127.0.0.1:8000/robots/update"
#SEND_COMMAND_ENDPOINT = "http://127.0.0.1:8000/send_command"
#HEALTH_ENDPOINT = "http://127.0.0.1:8000/health"
#WEBSOCKET_ENDPOINT = "ws://127.0.0.1:8000/ws"

REQUEST_TIMEOUT = (3.05, 5)
USER_DATA_FILE = "important.json"
GLOBAL_MODE = False
HEAT_RETRIES_LEFT = 6
SERVER_WARMED = False

myMC = schoolName = city = state = None
do_not_disturb = True
websocket_started = False
disconnectMonitor = None
rxCharacteristic = "452af57e-ad27-422c-88ae-76805ea641a9"
txCharacteristic = "266d9d74-3e10-4fcd-88d2-cb63b5324d0c"
isFirstCommand = True
isConnected = False
disconnect = False
isLookingForBluetooth = True
XRPAddress = ""
devices = []
myScanner = BleakScanner()
feedbackEvent = asyncio.Event()

async def unlockCommandMutex(sender, data):
    print(f"received {data}")
    feedbackEvent.set()

try:
    from bleak.backends.winrt.util import allow_sta, uninitialize_sta

    allow_sta()  # undo the unwanted side effect
    uninitialize_sta()
except:
    # not Windows, so no problem
    pass

context = Context()
socket = context.socket(REP)
socket.bind("tcp://127.0.0.1:5555")

push_update_socket = context.socket(PUSH)
push_update_socket.bind("tcp://127.0.0.1:5556")
link = RobotLink(push_update_socket)
aprsUpdater = APRSUpdater(link)

# try:
#     from bleak.backends.winrt.util import allow_sta
#     # tell Bleak we are using a graphical user interface that has been properly
#     # configured to work with asyncio
#     allow_sta()
# except ImportError:
#     # other OSes and older versions of Bleak will raise ImportError which we
#     # can safely ignore
#     pass

def read_config():
    global myMC, schoolName, city, state, GLOBAL_MODE
    if not os.path.exists(USER_DATA_FILE):
        return
    try:
        with open(USER_DATA_FILE, "r") as f:
            data = json.load(f)
            myMC = data.get("id")
            schoolName = data.get("school")
            city = data.get("city")
            state = data.get("state")
            if all([myMC, schoolName, city, state]):
                GLOBAL_MODE = True
    except (json.JSONDecodeError, IOError):
        pass

def update_robot(doNotDisturb: bool):
    """
    Add/push changes into view
    """
    global do_not_disturb, SERVER_WARMED, GLOBAL_MODE

    if not GLOBAL_MODE:
        return

    type_str = None
    if shared_state.robotType == shared_state.RobotType.XRP:
        type_str = "XRP"
    elif shared_state.robotType == shared_state.RobotType.MBOT:
        type_str = "mBot"

    do_not_disturb = doNotDisturb
    robot_data = {
        "id": myMC,
        "schoolName": schoolName,
        "city": city,
        "state": state,
        "doNotDisturb": doNotDisturb,
        "robotType": type_str
    }

    try:
        requests.post(UPDATE_ROBOT_ENDPOINT, json=robot_data, timeout=REQUEST_TIMEOUT)
        SERVER_WARMED = True
    except Timeout as e:
        if SERVER_WARMED:
            raise RuntimeError("Server did not reply in time")
        else:
            raise RuntimeError("Server is warming up - this may take up to 1 minute")
    except RequestException as e:
        print(f"IN REQUEST EXCEPTION: {str(e)}")
        raise RuntimeError("A network error has occured")

    return {"status": "ok"}

def get_active_robots():
    try:
        resp = requests.get(ACTIVE_ROBOTS_ENDPOINT, timeout=REQUEST_TIMEOUT)
        return resp.json()
    except RequestException:
        return {"status": "ok", "active_robots": []}

def send_command(msg):
    """
    Precondition: msg must be consistent with 
    the Command class defined in models.py
    """
    global SERVER_WARMED
    try:
        requests.post(SEND_COMMAND_ENDPOINT, json=msg, timeout=REQUEST_TIMEOUT)
        SERVER_WARMED = True
    except Timeout:
        if SERVER_WARMED:
            raise RuntimeError("Server did not reply in time")
        else:
            raise RuntimeError("Server is warming up - this may take up to 1 minute")
    except RequestException:
        raise RuntimeError("A network error has occured")
    
def pair_with_bot(msg) -> bool:
    try:
        ser = Serial(port=msg['port'], baudrate=115200, bytesize=8, timeout=5, stopbits=STOPBITS_ONE)
        link.setSerial(ser)
        link.getSerial().write(bytearray([255, 85, 7, 0, 2, 5, 0, 0, 0, 0])) # type: ignore #stop sequence
    except Exception as e:
        raise RuntimeError(f"Error: {str(e)}")
    return True

# def check_rtlsdr():
#     try:
#         sdr = RtlSdr()
#         sdr.close()
#         return {"status": "ok"}
#     except Exception as e:
#         return {"status": "error", "err_msg": str(e)}
    
def send_aprs(msg):
    try:
        mycallsign = msg["callsign"]
        commands = msg["commands"]
        destination = msg["destination"]
        
        if (shared_state.robotType == shared_state.RobotType.XRP):
            formatted = [f"{c['direction'][0]} {c['amount']}" for c in commands]
        else:
            formatted = [f"{c['power']} {c['direction']} {c['time']}" for c in commands]
            
        payload = f"[{', '.join(formatted)}]"

        if platform.system() == "Windows":
            oscommand = f"echo {mycallsign}^^^>APDW16::{destination:<9}:{payload} | .\\gen_packets -a 25 -o aprs_commands.wav -"
        elif platform.system() == "Linux":
            oscommand = f"echo -n '{mycallsign}>APDW16::{destination:<9}:{payload}' | gen_packets -a 25 -o aprs_commands.wav -"
        
        os.system(oscommand)
        # PlaySound("./aprs_commands.wav")
    except Exception as e:
        raise RuntimeError(f"Error sending aprs: {str(e)}")

async def monitorBluetooth():
    global devices
    while True:
        if isLookingForBluetooth:
            devices = await myScanner.discover()
            #print(f"devices = {devices}")
        await asyncio.sleep(5)

async def handle_request(msg):
    global disconnectMonitor
    global XRPAddress
    global disconnect
    global isConnected
    global isLookingForBluetooth
    global myScanner
    global do_not_disturb
    match msg['type']:
        case "get_directory":
            """
            msg format: {
                'type': 'get_directory'
            }
            """
            try:
                return get_active_robots()
            except Exception as e:
                return {"status": "error", "err_msg": str(e)}
        case "remote_control":
            """
            msg format: {
                'type': 'remote_control',
                'receiver_id': ,
                'commands': List[RobotCommand]
                'cmdType': 'mBot' | 'XRP'
            }
            """
            print(msg)
            payload = {
                'sender_id': myMC,
                'receiver_id': msg['receiver_id'],
                'commands': msg['commands'],
                'cmdType': msg['commands'][0]['cmdType']
            }
            try:
                send_command(payload)
                return {"status": "ok"}
            except Exception as e:
                return {"status": "error", "err_msg": str(e)}
        case "user_data_update":
            """
            msg format: {
                'type': 'user_data_update'
                'doNotDisturb': bool
            }
            """
            read_config()
            try:
                update_robot(msg["doNotDisturb"])
                return {"status": "ok"}
            except Exception as e:
                return {"status": "error", "err_msg": str(e)}
        case "local_control":
            """
            msg format: {
                'type': 'local_control'
                'commands': [100 forward 2, 200 backwards 1]
            }
            """
            try:
                if (shared_state.robotType == shared_state.RobotType.MBOT):
                    link.postToSerialJson(msg['commands'])
                else:
                    shared_state.cmdQueue = msg["commands"]
                    print(f"cmdQueue should now be {shared_state.cmdQueue}")
            except (SerialException, RuntimeError) as e:
                return {"status": "error", "err_msg": str(e)}
            return {"status": "ok"}
        case "pair_connect":
            """
            msg format: {
                'type': 'pair_connect'
                'port': ,
            }
            """
            try:
                if "XRP" in msg["port"]:
                    changed = False
                    if shared_state.robotType == shared_state.RobotType.MBOT or shared_state.robotType is None:
                        changed = True

                    shared_state.robotType = shared_state.RobotType.XRP
                    isConnected = True
                    await myScanner.stop()
                    #asyncio.gather(XRPControl())
                    asyncio.create_task(XRPControl())

                    if changed:
                        update_robot(do_not_disturb)
                    return {"status": "ok", "robotType" : "XRP"}
                else:
                    changed = False
                    if shared_state.robotType == shared_state.RobotType.XRP or shared_state.robotType is None:
                        changed = True

                    shared_state.robotType = shared_state.RobotType.MBOT
                    pair_with_bot(msg)
                    
                    if changed:
                        update_robot(do_not_disturb)
                    disconnectMonitor = USBDisconnectWatcher(msg['port'], aprsUpdater, push_update_socket, link)
                    disconnectMonitor.start()
                    return {"status": "ok", "robotType" : "mBot"}
                
                # if (disconnectMonitor.vid == "1a86"):
                #     robotType = RobotType.MBOT
                #     return {"status": "ok", "robotType" : "mBot"}
                # else:
                #     robotType = RobotType.XRP
                #     return {"status": "ok", "robotType" : "XRP"}
                
            except Exception as e:
                return {"status": "error", "err_msg": str(e)}     
        case "pair_disconnect":
            try:
                update_robot(do_not_disturb)
                if (shared_state.robotType == shared_state.RobotType.MBOT):
                    link.closeSerial()
                    disconnectMonitor.stop() # type: ignore
                    return {"status": "ok"}
                else:
                    disconnect = True
                    return {"status": "ok"}
            except Exception as e:
                return {"status": "error", "err_msg": f"Error: {str(e)}"}   
        case "get_ports":
            ports = [str(port.device) for port in comports()]
            devices = myScanner.discovered_devices
            # ports = ["COM4"]
            # XRP = BLEDevice(address="28:CD:C1:16:DA:9A", name="XRProver", details="XRProver")
            for d in devices:
                if (d.name is not None and "XRP" in d.name):
                    XRP = d
                    XRPAddress = d.address
                    break
            try:    
                ports.append(XRP.name) # type: ignore
            except:
                print("XRP not found!")
            finally:
                pass

            payload = {
                "status": "ok",
                "ports": ports
            }
            return payload
        case "send_aprs":
            try:
                send_aprs(msg)
                return {"status": "ok"}
            except Exception as e:
                return {"status": "error", "err_msg": str(e)}
        case "receive_aprs":
            if platform.system() == "linux":
                # check = check_rtlsdr()
                # if check["status"] == "error":
                #     return check
                pass

            try:
                aprsUpdater.startAPRSprocesses()
            except Exception as e:
                return {"status": "error", "err_msg": str(e)}

            if(platform.system() == "Linux"):
                thread = Thread(target=aprsUpdater.checkAPRSUpdates_Linux, daemon=True)
            else:
                thread = Thread(target=aprsUpdater.checkAPRSUpdates, daemon=True)

            thread.start()
            return {"status": "ok"}
        case "stop_aprs_receive":
            aprsUpdater.stop()
            return {"status": "ok"}
        case "end_program":
            try:
                socket.send_json({"status": "ok"})
                aprsUpdater.stop()
                context.destroy()
                sys.exit(0)
            except Exception as e:
                socket.send_json({"status": "error"})
                print(f"[DEBUG] Error {str(e)}")

async def zmq_loop():
    while True:
        try:
            msg = await asyncio.get_running_loop().run_in_executor(None, socket.recv_json) # Run blocking recv_json() in separate thread
            print(f"[ZMQ] Received: {msg}")
            print(f"cmd = {shared_state.cmdQueue}")
            response = await handle_request(msg)
            print(f"[ZMQ] Response: {response}")
            socket.send_json(response)
        except Exception as e:
            socket.send_json({"status": "error", "detail": str(e)})
async def main():
    read_config()
    # await asyncio.gather(
    #     zmq_loop(),
    #     auto_reconnect_loop(),
    #     health_check()
    # )
    await myScanner.start()
    await asyncio.gather(
        zmq_loop(),
        health_check(),
        auto_reconnect_loop(),
        printAllCoroutines()
    )

def ping_health_endpoint():
    try:
        requests.get(HEALTH_ENDPOINT, timeout=REQUEST_TIMEOUT)
        print("[DEBUG] keep alive ping succeeded")
        return True
    except Exception as e:
        print("[DEBUG] Keep alive ping failed")
        return False

async def health_check():
    global HEAT_RETRIES_LEFT, SERVER_WARMED
    
    while True:
        if GLOBAL_MODE and HEAT_RETRIES_LEFT > 0:
            if ping_health_endpoint():
                SERVER_WARMED = True
                HEAT_RETRIES_LEFT = 0
            HEAT_RETRIES_LEFT -= 1
            await asyncio.sleep(10)

        elif GLOBAL_MODE:
            ping_health_endpoint() # Prevent sleeping
            await asyncio.sleep(600)
        else:
            await asyncio.sleep(10)

async def printAllCoroutines():
    while True:
        #print(f"All currently running tasks: {[t.get_coro().__name__ for t in asyncio.all_tasks()]}") # type: ignore
        await asyncio.sleep(10)

async def auto_reconnect_loop():
    while True:
        if GLOBAL_MODE and not websocket_started:
            print("[DEBUG] Retry websocket connection")
            await connect_to_ws()
        #print(f"All currently running tasks: {[t.get_coro() for t in asyncio.all_tasks()]}")
        await asyncio.sleep(10)

async def connect_to_ws():
    global websocket_started
    
    type_str = None
    if shared_state.robotType == shared_state.RobotType.XRP:
        type_str = "XRP"
    elif shared_state.robotType == shared_state.RobotType.MBOT:
        type_str = "mBot"

    try:
        websocket = await asyncio.wait_for(websockets.connect(WEBSOCKET_ENDPOINT), timeout=3)
        async with websocket: # type: ignore
            print("[DEBUG] Opening socket")
            await websocket.send(json.dumps({
                "id": myMC,
                "schoolName": schoolName,
                "city": city,
                "state": state,
                "doNotDisturb": do_not_disturb,
                "robotType": type_str
            }))
            websocket_started = True
            while True:
                msg = await websocket.recv()
                try:
                    data = json.loads(msg)
                    if data["type"] == "command":
                        type = "mBot" if shared_state.robotType == shared_state.RobotType.MBOT else "XRP"
                        if shared_state.robotType is not None and type == data["cmdType"]:
                            if type == "mBot":
                                print("posting to Serial")
                                link.postToSerialJson(data["commands"])
                            else:
                                shared_state.cmdQueue = data["commands"]
                        print("[WebSocket] Received:", data)
                except json.JSONDecodeError:
                    print("[Error] JSON decode error")
    except Exception as e:
        print(f"[WebSocket] Connection failed: {str(e)}")
        websocket_started = False

async def sendXRPCommand(client: BleakClient, cmd: Buffer):
    await client.write_gatt_char(rxCharacteristic, cmd, response=True)

async def XRPControl():
    global isFirstCommand, isConnected, disconnect, XRPAddress, push_update_socket
    print("Trying to connect to XRP " + XRPAddress)

    async with BleakClient(XRPAddress) as client:
        print("Connected to XRP " + XRPAddress)
        await client.start_notify(txCharacteristic, callback=unlockCommandMutex)
        while True:
            await asyncio.sleep(0.5)

            if shared_state.cmdQueue:
                isFirstCommand = True
                current_commands = list(shared_state.cmdQueue)
                shared_state.cmdQueue.clear()
                push_update_socket.send_json({"type": "command", "cmdType": "XRP", "commands": current_commands})

                for c in current_commands:
                    print(f"Currently executing {c}")
                    if not isFirstCommand:
                        await feedbackEvent.wait()
                        feedbackEvent.clear()

                    isFirstCommand = False
                    cmd = ("1 " + c["direction"][0] + " " + str(c["amount"])).encode("utf-8")
                    if (c["direction"][0] == "d"):
                        await asyncio.sleep(float(c["amount"]))
                        feedbackEvent.set()
                    else:
                        print(f"Sending {cmd}")
                        await sendXRPCommand(client=client, cmd=cmd)

            if disconnect:
                print("Disconnecting")
                shared_state.cmdQueue.clear()
                disconnect = False
                await client.disconnect()
                return

asyncio.run(main())
