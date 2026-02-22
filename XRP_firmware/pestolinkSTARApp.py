from micropython import const
import bluetooth
import random
import struct
import time


_IRQ_CENTRAL_CONNECT = const(1)
_IRQ_CENTRAL_DISCONNECT = const(2)
_IRQ_GATTS_WRITE = const(3)

_FLAG_READ = const(0x0002)
_FLAG_WRITE_NO_RESPONSE = const(0x0004)
_FLAG_WRITE = const(0x0008)
_FLAG_NOTIFY = const(0x0010)

_UART_UUID = bluetooth.UUID("27df26c5-83f4-4964-bae0-d7b7cb0a1f54")
_UART_TX = (
    bluetooth.UUID("266d9d74-3e10-4fcd-88d2-cb63b5324d0c"),
    _FLAG_READ | _FLAG_NOTIFY,
)
_UART_RX = (
    bluetooth.UUID("452af57e-ad27-422c-88ae-76805ea641a9"),
    _FLAG_WRITE | _FLAG_WRITE_NO_RESPONSE,
)
_UART_SERVICE = (
    _UART_UUID,
    (_UART_TX, _UART_RX),
)

_ADV_TYPE_FLAGS = const(0x01)
_ADV_TYPE_NAME = const(0x09)
_ADV_TYPE_UUID16_COMPLETE = const(0x3)
_ADV_TYPE_UUID32_COMPLETE = const(0x5)
_ADV_TYPE_UUID128_COMPLETE = const(0x7)
_ADV_TYPE_UUID16_MORE = const(0x2)
_ADV_TYPE_UUID32_MORE = const(0x4)
_ADV_TYPE_UUID128_MORE = const(0x6)
_ADV_TYPE_APPEARANCE = const(0x19)


# Generate a payload to be passed to gap_advertise(adv_data=...).
def advertising_payload(limited_disc=False, br_edr=False, name=None, services=None, appearance=0):
    payload = bytearray()

    def _append(adv_type, value):
        nonlocal payload
        payload += struct.pack("BB", len(value) + 1, adv_type) + value

    _append(
        _ADV_TYPE_FLAGS,
        struct.pack("B", (0x01 if limited_disc else 0x02) + (0x18 if br_edr else 0x04)),
    )

    if name:
        _append(_ADV_TYPE_NAME, name)

    if services:
        for uuid in services:
            b = bytes(uuid)
            if len(b) == 2:
                _append(_ADV_TYPE_UUID16_COMPLETE, b)
            elif len(b) == 4:
                _append(_ADV_TYPE_UUID32_COMPLETE, b)
            elif len(b) == 16:
                _append(_ADV_TYPE_UUID128_COMPLETE, b)

    # See org.bluetooth.characteristic.gap.appearance.xml
    if appearance:
        _append(_ADV_TYPE_APPEARANCE, struct.pack("<h", appearance))

    return payload


def decode_field(payload, adv_type):
    i = 0
    result = []
    while i + 1 < len(payload):
        if payload[i + 1] == adv_type:
            result.append(payload[i + 2 : i + payload[i] + 1])
        i += 1 + payload[i]
    return result


def decode_name(payload):
    n = decode_field(payload, _ADV_TYPE_NAME)
    return str(n[0], "utf-8") if n else ""


def decode_services(payload):
    services = []
    for u in decode_field(payload, _ADV_TYPE_UUID16_COMPLETE):
        services.append(bluetooth.UUID(struct.unpack("<h", u)[0]))
    for u in decode_field(payload, _ADV_TYPE_UUID32_COMPLETE):
        services.append(bluetooth.UUID(struct.unpack("<d", u)[0]))
    for u in decode_field(payload, _ADV_TYPE_UUID128_COMPLETE):
        services.append(bluetooth.UUID(u))
    return services

class PestoLinkAgent:
    def __init__(self, name):
        sliced_name = name[:8] #only use the first 8 characters in the name, otherwise code will crash
        self._ble = bluetooth.BLE()
        self._ble.active(True)
        self._ble.irq(self._irq)
        ((self._handle_tx, self._handle_rx),) = self._ble.gatts_register_services((_UART_SERVICE,))
        self._connections = set()
        self._payload = advertising_payload(name=sliced_name, services=[_UART_UUID])
        self._byte_list = [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]
        self._advertise()
        self.last_telemetry_ms = 0

    def _irq(self, event, data):
        # Track connections so we can send notifications.
        if event == _IRQ_CENTRAL_CONNECT:
            conn_handle, _, _ = data
            #print("New connection")
            self._connections.add(conn_handle)
        elif event == _IRQ_CENTRAL_DISCONNECT:
            conn_handle, _, _ = data
            #print("Disconnected")
            self._connections.discard(conn_handle)
            # Start advertising again to allow a new connection.
            self._advertise()
        elif event == _IRQ_GATTS_WRITE:
            conn_handle, value_handle = data
            value = self._ble.gatts_read(value_handle)
            if value_handle == self._handle_rx:
                self.on_write(value)

    def send(self, data):
        for conn_handle in self._connections:
            self._ble.gatts_notify(conn_handle, self._handle_tx, data)

    def is_connected(self):
        return len(self._connections) > 0

    def _advertise(self, interval_us=50000):
        #print("Starting advertising")
        self._ble.gap_advertise(interval_us, adv_data=self._payload)

    def on_write(self, value):
        _raw_byte_list = [byte for byte in value]
        self._byte_list = _raw_byte_list
        
    def get_raw_axis(self, axis_num):
        if axis_num < 0 or axis_num > 3 or self._byte_list == None:
            return 127
        else:
            return self._byte_list[1 + axis_num]

    def get_axis(self, axis_num):
        raw_axis = self.get_raw_axis(axis_num)
        if raw_axis == 127:
            return 0
        else:
            return (raw_axis / 127.5) - 1
        
    def get_button(self, button_num):
        if self._byte_list == None:
            return False
        
        raw_buttons = (self._byte_list[6] << 8) + self._byte_list[5]
        if ((raw_buttons >> (button_num)) & 0x01):
            return True
        else:
            return False
            
    def telemetryPrint(self, telemetry, hex_code):
        if self.last_telemetry_ms + 500 > time.ticks_ms():
            return

        result = bytearray(11)
        
        # Copy up to 8 characters from telemetry
        for i in range(8):
            if i < len(telemetry):
                result[i] = ord(telemetry[i])
            else:
                result[i] = 0
        
        # Adjust pointer if the hex code starts with "0x"
        if hex_code.startswith("0x"):
            hex_code = hex_code[2:]
        
        try:
            color = int(hex_code, 16)
        except ValueError:
            color = 0  # Default to 0 if conversion fails
        
        #debug: print(str(hex_code) + " " + str(color))
        
        result[8] = (color >> 16) & 0xFF
        result[9] = (color >> 8) & 0xFF
        result[10] = color & 0xFF
        
        self.send(result)  # Assuming BLE characteristic write
        self.last_telemetry_ms = time.ticks_ms()
        
    def telemetryPrintBatteryVoltage(self, battery_voltage):
        voltage_string = "{:.2f} V".format(battery_voltage)
        
        if battery_voltage >= 7.6:
            self.telemetryPrint(voltage_string, "00FF00")
        elif battery_voltage >= 7:
            self.telemetryPrint(voltage_string, "FFFF00")
        else:
            self.telemetryPrint(voltage_string, "FF0000")
