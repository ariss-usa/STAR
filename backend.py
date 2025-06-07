import asyncio
import platform
from threading import Thread
import time
import requests
from requests import RequestException, Timeout
from zmq import PUSH, Context
from zmq import REP
from aprsListener import APRSUpdater
from serial import Serial, SerialException
from serial import STOPBITS_ONE
from serial.tools.list_ports import comports
import os
import websockets
import json
from rtlsdr import RtlSdr
from playsound3 import playsound
from DisconnectMonitor import USBDisconnectWatcher
import sys
from robot_link import RobotLink

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

context = Context()
socket = context.socket(REP)
socket.bind("tcp://127.0.0.1:5555")

push_update_socket = context.socket(PUSH)
push_update_socket.bind("tcp://127.0.0.1:5556")
link = RobotLink(push_update_socket)
aprsUpdater = APRSUpdater(link)

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
    global do_not_disturb, SERVER_WARMED
    do_not_disturb = doNotDisturb
    robot_data = {
        "id": myMC,
        "schoolName": schoolName,
        "city": city,
        "state": state,
        "doNotDisturb": doNotDisturb
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
        link.getSerial().write(bytearray([255, 85, 7, 0, 2, 5, 0, 0, 0, 0])) #stop sequence
    except Exception as e:
        raise RuntimeError(f"Error: {str(e)}")
    return True

def check_rtlsdr():
    try:
        sdr = RtlSdr()
        sdr.close()
        return {"status": "ok"}
    except Exception as e:
        return {"status": "error", "err_msg": str(e)}
    
def send_aprs(msg):
    try:
        mycallsign = msg["callsign"]
        commands = msg["commands"]
        destination = msg["destination"]

        formatted = [f"{c['power']} {c['direction']} {c['time']}" for c in commands]
        payload = f"[{', '.join(formatted)}]"

        if platform.system() == "Windows":
            oscommand = f"echo {mycallsign}^^^>{destination}: {payload} | .\\gen_packets -a 25 -o aprs_commands.wav -"
        elif platform.system() == "Linux":
            oscommand = f"echo -n '{mycallsign}>{destination}: {payload}' | gen_packets -a 25 -o aprs_commands.wav -"
        
        os.system(oscommand)
        playsound("./aprs_commands.wav")
    except Exception as e:
        raise RuntimeError(f"Error sending aprs: {str(e)}")
        
async def handle_request(msg):
    global disconnectMonitor
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
            }
            """
            print(msg)
            payload = {
                'sender_id': myMC,
                'receiver_id': msg['receiver_id'],
                'commands': msg['commands']
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
                link.postToSerialJson(msg['commands'])
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
                pair_with_bot(msg)
                disconnectMonitor = USBDisconnectWatcher(msg['port'], aprsUpdater, push_update_socket, link)
                disconnectMonitor.start()
                return {"status": "ok"}
            except Exception as e:
                return {"status": "error", "err_msg": str(e)}
            
        case "pair_disconnect":
            try:
                link.closeSerial()
                disconnectMonitor.stop()
                return {"status": "ok"}
            except Exception as e:
                return {"status": "error", "err_msg": f"Error: {str(e)}"}
        
        case "get_ports":
            ports = [str(port.device) for port in comports()]
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
                check = check_rtlsdr()
                if check["status"] == "error":
                    return check

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
            response = await handle_request(msg)
            print(f"[ZMQ] Response: {response}")
            socket.send_json(response)
        except Exception as e:
            socket.send_json({"status": "error", "detail": str(e)})


async def main():
    read_config()
    await asyncio.gather(
        zmq_loop(),
        auto_reconnect_loop(),
        health_check()
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

async def auto_reconnect_loop():
    while True:
        if GLOBAL_MODE and not websocket_started:
            print("[DEBUG] Retry websocket connection")
            await connect_to_ws()
        await asyncio.sleep(10)

async def connect_to_ws():
    global websocket_started

    try:
        websocket = await asyncio.wait_for(websockets.connect(WEBSOCKET_ENDPOINT), timeout=3)

        async with websocket:
            print("[DEBUG] Opening socket")
            await websocket.send(json.dumps({
                "id": myMC,
                "schoolName": schoolName,
                "city": city,
                "state": state,
                "doNotDisturb": do_not_disturb
            }))
            websocket_started = True
            while True:
                msg = await websocket.recv()
                try:
                    data = json.loads(msg)
                    if data["type"] == "command":
                        print("posting to Serial")
                        link.postToSerialJson(data["commands"])
                        print("[WebSocket] Received:", data)
                except json.JSONDecodeError:
                    print("[Error] JSON decode error")
    except Exception as e:
        print(f"[WebSocket] Connection failed: {str(e)}")
        websocket_started = False

asyncio.run(main())