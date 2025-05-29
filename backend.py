import asyncio
import requests
from zmq import Context
from zmq import REP
import helper
from serial import Serial
from serial import STOPBITS_ONE
from serial import SerialException
from serial.tools.list_ports import comports
import os
import websockets
import json

ACTIVE_ROBOTS_ENDPOINT = "http://localhost:8000/robots/active"
UPDATE_ROBOT_ENDPOINT = "http://localhost:8000/robots/update"
SEND_COMMAND_ENDPOINT = "http://localhost:8000/send_command"
WEBSOCKET_ENDPOINT = "ws://localhost:8000/ws"
USER_DATA_FILE = "important.txt"
GLOBAL_MODE = False

myMC = schoolName = city = state = None
do_not_disturb = True
websocket_started = False

context = Context()
socket = context.socket(REP)
socket.bind("tcp://127.0.0.1:5555")

def read_config():
    global myMC, schoolName, city, state, GLOBAL_MODE
    if not os.path.exists(USER_DATA_FILE):
        return

    with open(USER_DATA_FILE, "r") as f:
        myMC = f.readline().strip()
        schoolName = f.readline().strip()
        city = f.readline().strip()
        state = f.readline().strip()
        GLOBAL_MODE = True

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
        resp = requests.post(UPDATE_ROBOT_ENDPOINT, json=robot_data)
        print("update status", resp.status_code)
    except Exception as e:
        print("Failed to update robot:", e)

def get_active_robots():
    try:
        resp = requests.get(ACTIVE_ROBOTS_ENDPOINT)
        return resp.json()
    except Exception as e:
        print("Failed to get active robots:", e)
        return {"status": "error"}

def send_command(msg):
    """
    Precondition: msg must be consistent with 
    the Command class defined in models.py
    """
    try:
        resp = requests.post(SEND_COMMAND_ENDPOINT, json=msg)
        print("status", resp.status_code)
        return {"status": "ok"}
    except Exception as e:
        print("Failed to send command:", e)
        return {"status": "error"}
    
def pair_with_bot(msg) -> bool:
    try:
        ser = Serial(port=msg['port'], baudrate=115200, bytesize=8, timeout=5, stopbits=STOPBITS_ONE)
        helper.setSerial(ser)
        helper.getSerial().write(bytearray([255, 85, 7, 0, 2, 5, 0, 0, 0, 0])) #stop sequence
    except SerialException:
        return False
    return True
    
async def handle_request(msg):
    match msg['type']:
        case "get_directory":
            """
            msg format: {
                'type': 'get_directory'
            }
            """
            return get_active_robots()
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
            return send_command(payload)
        case "user_data_update":
            """
            msg format: {
                'type': 'user_data_update'
                'doNotDisturb': bool
            }
            """
            read_config()
            print(f"[DEBUG] websocket started: {websocket_started}")
            print(f"[DEBUG] global mode: {GLOBAL_MODE}")
            if not websocket_started and GLOBAL_MODE:
                asyncio.create_task(connect_to_ws())

            update_robot(msg["doNotDisturb"])

            return {"status": "ok"}
        case "local_control":
            """
            msg format: {
                'type': 'local_control'
                'commands': [100 forward 2, 200 backwards 1]
            }
            """
            helper.postToSerialJson(msg['commands'])
            return {"status": "ok"}
        case "pair_connect":
            """
            msg format: {
                'type': 'pair_connect'
                'port': ,
            }
            """
            if pair_with_bot(msg):
                return {"status": "ok"}
            else:
                return {"status": "error"}
            
        case "pair_disconnect":
            helper.setSerial(None)
            return {"status": "ok"}
        
        case "get_ports":
            ports = [str(port.device) for port in comports()]
            payload = {
                "status": "ok",
                "ports": ports
            }
            return payload

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
    #tasks = [zmq_loop()]
    tasks = [asyncio.create_task(zmq_loop())]
    if GLOBAL_MODE:
        print("scheduling websocket connect")
        tasks.append(asyncio.create_task(connect_to_ws()))
    else:
        print("skipping websocket connection")
    await asyncio.gather(*tasks)

async def connect_to_ws():
    global websocket_started

    try:
        async with websockets.connect(WEBSOCKET_ENDPOINT) as websocket:
            print("Opening socket")
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
                data = json.loads(msg)
                if data["type"] == "command":
                    print("posting to Serial")
                    helper.postToSerialJson(data["commands"])

                print("[WebSocket] Received:", data)
    except Exception as e:
        print(f"[WebSocket] Connection failed: {e}")
        websocket_started = False

asyncio.run(main())