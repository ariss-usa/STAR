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

ACTIVE_ROBOTS_ENDPOINT = "http://localhost:8000/robots/active"
UPDATE_ROBOT_ENDPOINT = "http://localhost:8000/robots/update"
SEND_COMMAND_ENDPOINT = "http://localhost:8000/send_command"
USER_DATA_FILE = "important.txt"
GLOBAL_MODE = False

myMC = schoolName = city = state = None
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
            update_robot(msg)
            return {"status": "ok"}
        case "local_control":
            """
            msg format: {
                'type': 'local_control'
                'commands': [100 forward 2, 200 backwards 1]
            }
            """
            #TODO: update postToSerial to use its own serial port (not take in one since we set it)
            helper.postToSerial(helper.getSerial(), msg['commands'])
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

async def main_loop():
    read_config()
    while True:
        try:
            msg = socket.recv_json()
            print(f"[ZMQ] Received: {msg}")
            response = await handle_request(msg)
            print(f"[ZMQ] Response: {response}")
            socket.send_json(response)
        except Exception as e:
            socket.send_json({"status": "error", "detail": str(e)})

asyncio.run(main_loop())