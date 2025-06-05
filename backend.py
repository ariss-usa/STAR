import asyncio
import platform
from threading import Thread
import requests
from requests import RequestException, Timeout
from zmq import PUSH, Context
from zmq import REP
from aprsListener import APRSUpdater
import helper
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

ACTIVE_ROBOTS_ENDPOINT = "https://star-44oa.onrender.com/robots/active"
UPDATE_ROBOT_ENDPOINT = "https://star-44oa.onrender.com/robots/update"
SEND_COMMAND_ENDPOINT = "https://star-44oa.onrender.com/send_command"
WEBSOCKET_ENDPOINT = "wss://star-44oa.onrender.com/ws"
REQUEST_TIMEOUT = (3.05, 5)
USER_DATA_FILE = "important.json"
GLOBAL_MODE = False

myMC = schoolName = city = state = None
do_not_disturb = True
websocket_started = False
aprsUpdater = APRSUpdater()
disconnectMonitor = None

context = Context()
socket = context.socket(REP)
socket.bind("tcp://127.0.0.1:5555")

push_update_socket = context.socket(PUSH)
push_update_socket.bind("tcp://127.0.0.1:5556")

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
    global do_not_disturb
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
    except Timeout:
        raise RuntimeError("Server did not reply in time")
    except RequestException:
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
    try:
        requests.post(SEND_COMMAND_ENDPOINT, json=msg, timeout=REQUEST_TIMEOUT)
    except Timeout:
        raise RuntimeError("Server did not reply in time")
    except RequestException:
        raise RuntimeError("A network error has occured")
    
def pair_with_bot(msg) -> bool:
    try:
        ser = Serial(port=msg['port'], baudrate=115200, bytesize=8, timeout=5, stopbits=STOPBITS_ONE)
        helper.setSerial(ser)
        helper.getSerial().write(bytearray([255, 85, 7, 0, 2, 5, 0, 0, 0, 0])) #stop sequence
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
            oscommand = f"echo {mycallsign}^^^>{destination}: {payload} | gen_packets -a 25 -o aprs_commands.wav -"
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
                helper.postToSerialJson(msg['commands'])
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
                disconnectMonitor = USBDisconnectWatcher(msg['port'], aprsUpdater, push_update_socket)
                disconnectMonitor.start()
                return {"status": "ok"}
            except Exception as e:
                return {"status": "error", "err_msg": str(e)}
            
        case "pair_disconnect":
            try:
                helper.closeSerial()
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
                thread = Thread(target=aprsUpdater.checkAPRSUpdates_RTLSDR)
            else:
                thread = Thread(target=aprsUpdater.checkAPRSUpdates)

            thread.start()
            return {"status": "ok"}
        case "stop_aprs_receive":
            aprsUpdater.stop()
            return {"status": "ok"}
        
        case "end_program":
            try:
                socket.send_json({"status": "ok"})
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
        auto_reconnect_loop()
    )

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
                        push_update_socket.send_json(data)
                        helper.postToSerialJson(data["commands"])
                        print("[WebSocket] Received:", data)
                except json.JSONDecodeError:
                    print("[Error] JSON decode error")
    except Exception as e:
        print(f"[WebSocket] Connection failed: {str(e)}")
        websocket_started = False

asyncio.run(main())