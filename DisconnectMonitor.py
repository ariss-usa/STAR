from usbmonitor import USBMonitor
from usbmonitor.attributes import ID_VENDOR_ID, ID_MODEL_ID
from serial.tools import list_ports
import threading
from robot_link import RobotLink

class USBDisconnectWatcher:
    def __init__(self, port_name, aprsUpdater, push_socket, link):
        self.port_name = port_name
        self.vid, self.pid = self._get_vid_pid(port_name)
        self.monitor = USBMonitor()
        self.aprsUpdater = aprsUpdater
        self.push_socket = push_socket
        self.link = link

    def _get_vid_pid(self, port_name):
        for p in list_ports.comports():
            if p.device == port_name:
                return format(p.vid, '04x'), format(p.pid, '04x')
        return None, None

    def _on_disconnect(self, device_id, device_info):
        if (device_info.get(ID_VENDOR_ID, "").lower() == self.vid and
            device_info.get(ID_MODEL_ID, "").lower() == self.pid):
            print(f"[USB Watchdog] Matched disconnect on port {self.port_name}")
            threading.Timer(0, self.stop).start()
            self.link.closeSerial()
            self.aprsUpdater.stop()

            if self.push_socket:
                self.push_socket.send_json({"type": "usb_disconnect"})
            else:
                print("[USB Watchdog] Warning: Push socket not initialized")


    def start(self):
        if not self.vid or not self.pid:
            print(f"[USB Watchdog] Could not determine VID/PID for {self.port_name}")
            return

        self.monitor.start_monitoring(
            on_connect=None,
            on_disconnect=self._on_disconnect
        )
        print(f"[USB Watchdog] Monitoring USB disconnect for port: {self.port_name}")

    def stop(self):
        self.monitor.stop_monitoring()