# Import necessary modules
import time
import network
import socket

from XRPLib.defaults import *
from XRPLib.board import Board
from pestolinkSTARApp import PestoLinkAgent

# Choose the name your robot shows up as in the Bluetooth pairing menu
# Name should be 8 characters max!
robot_name = "XRProver"

# Optional WiFi mode. Leave blank to disable WiFi control.
WIFI_SSID = ""
WIFI_PASSWORD = ""
WIFI_PORT = 3540
WIFI_DISCOVERY_PORT = 3541
DISCOVERY_REQUEST = "STAR_XRP_DISCOVER"
DISCOVERY_RESPONSE_PREFIX = "STAR_XRP_HERE:"

# Create an instance of the PestoLinkAgent class
pestolink = PestoLinkAgent(robot_name)

board = Board.get_default_board()
currentlyMoving = False
active_cmd = None


def setup_wifi_server():
    if not WIFI_SSID:
        print("WiFi control disabled (no WIFI_SSID set)")
        return None, None, None

    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    if not wlan.isconnected():
        print("Connecting to WiFi...")
        wlan.connect(WIFI_SSID, WIFI_PASSWORD)
        for _ in range(50):
            if wlan.isconnected():
                break
            time.sleep(0.2)

    if not wlan.isconnected():
        print("WiFi connection failed; BLE-only mode")
        return None, None, None

    ip = wlan.ifconfig()[0]
    print("WiFi connected, XRP IP:", ip)

    server = socket.socket()
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(("0.0.0.0", WIFI_PORT))
    server.listen(1)
    server.settimeout(0)

    discovery_server = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    discovery_server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    discovery_server.bind(("0.0.0.0", WIFI_DISCOVERY_PORT))
    discovery_server.settimeout(0)

    print(f"WiFi command server listening on {ip}:{WIFI_PORT}")
    return server, discovery_server, ip




def service_discovery(discovery_server, wifi_ip):
    if discovery_server is None or wifi_ip is None:
        return

    try:
        payload, addr = discovery_server.recvfrom(128)
    except OSError:
        return

    req = payload.decode("utf-8").strip()
    if req != DISCOVERY_REQUEST:
        return

    response = f"{DISCOVERY_RESPONSE_PREFIX}{wifi_ip}:{WIFI_PORT}"
    try:
        discovery_server.sendto(response.encode("utf-8"), addr)
    except OSError:
        pass

def read_ble_command():
    if not pestolink.is_connected():
        return None

    if len(pestolink._byte_list) == 0 or pestolink._byte_list[0] != 0x31:
        return None

    try:
        cmd = bytes(pestolink._byte_list).decode("utf-8").strip()
        pestolink._byte_list[0] = 0
        return cmd
    except Exception:
        pestolink._byte_list[0] = 0
        return None


def execute_command(raw_cmd):
    global currentlyMoving, active_cmd

    parts = raw_cmd.split()
    if len(parts) < 3 or parts[0] != "1":
        return

    currentlyMoving = True
    active_cmd = parts

    if parts[1] == "f":
        drivetrain.straight(float(parts[2]), 0.75)
    elif parts[1] == "r":
        drivetrain.turn(-float(parts[2]), 0.75)
    elif parts[1] == "b":
        drivetrain.straight(-float(parts[2]), 0.75)
    elif parts[1] == "l":
        drivetrain.turn(float(parts[2]), 0.75)
    elif parts[1] == "a":
        servo_one.set_angle(float(parts[2]))
        currentlyMoving = False


wifi_server, discovery_server, wifi_ip = setup_wifi_server()
wifi_conn = None  # persistent WiFi connection

# Start an infinite loop
while True:
    service_discovery(discovery_server, wifi_ip)

    # Accept a new WiFi connection only when we don't have one
    if wifi_server is not None and wifi_conn is None:
        try:
            wifi_conn, _ = wifi_server.accept()
            wifi_conn.settimeout(0)  # non-blocking reads
            print("WiFi client connected")
        except OSError:
            pass

    ble_cmd = read_ble_command()

    # Read from the persistent WiFi connection
    wifi_cmd = None
    if wifi_conn is not None:
        try:
            data = wifi_conn.recv(128)
            if data:
                cmd = data.decode("utf-8").strip()
                print(f"WiFi recv: '{cmd}'")
                if cmd and cmd != "ping":
                    wifi_cmd = cmd
            else:
                # Remote closed the connection
                print("WiFi client disconnected")
                wifi_conn.close()
                wifi_conn = None
        except OSError as e:
            if e.args[0] != 11:  # 11 = EAGAIN (no data yet); anything else is a real error
                print(f"WiFi recv error: {e}")
                try:
                    wifi_conn.close()
                except OSError:
                    pass
                wifi_conn = None

    if ble_cmd is not None or wifi_cmd is not None:
        board.led_on()
        command_text = ble_cmd if ble_cmd is not None else wifi_cmd
        print("Executing command:", command_text)
        execute_command(command_text)
        # Servo/instant commands finish synchronously (currentlyMoving stays False),
        # so ack here — drive commands are handled by the speed-check below
        if not currentlyMoving:
            pestolink.send(b"Ready for next command")
            if wifi_conn is not None:
                print("WiFi ack: sending (instant/servo)")
                try:
                    wifi_conn.send(b"Ready for next command\n")
                except OSError:
                    try:
                        wifi_conn.close()
                    except OSError:
                        pass
                    wifi_conn = None
    elif not pestolink.is_connected() and wifi_server is None:
        board.led_off()
        servo_one.set_angle(60)
        drivetrain.arcade(0, 0)

    if currentlyMoving and drivetrain.left_motor.get_speed() <= 1 and drivetrain.right_motor.get_speed() <= 1:
        currentlyMoving = False
        print(f"Finished command {active_cmd}, ready for next command")
        pestolink.send(b"Ready for next command")
        # Mirror the BLE ack over WiFi so the backend knows the command is done
        if wifi_conn is not None:
            print("WiFi ack: sending (drive complete)")
            try:
                wifi_conn.send(b"Ready for next command\n")
            except OSError:
                try:
                    wifi_conn.close()
                except OSError:
                    pass
                wifi_conn = None

    time.sleep(0.1)
