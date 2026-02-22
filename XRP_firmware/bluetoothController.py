# Import necessary modules
from machine import Pin, ADC
import bluetooth
import time
import json
import math

from XRPLib.defaults import *
from XRPLib.board import Board
from pestolinkSTARApp import PestoLinkAgent

#Choose the name your robot shows up as in the Bluetooth paring menu
#Name should be 8 characters max!
robot_name = "XRProver"

# Create an instance of the PestoLinkAgent class
pestolink = PestoLinkAgent(robot_name)

board = Board.get_default_board()
currentlyMoving = False

# Start an infinite loop
while True:
    if pestolink.is_connected():  # Check if a BLE connection is established
        board.led_on()
        print("Current byte list: " + str(bytes(pestolink._byte_list).decode("utf-8")))
        if len(pestolink._byte_list) != 0 and pestolink._byte_list[0] == 0x31:
            currentlyMoving = True
            cmd = bytes(pestolink._byte_list).decode("utf-8").split()
            print(cmd)
            if (cmd[1] == "f"):
                drivetrain.straight(float(cmd[2]), 0.75)
            elif (cmd[1] == "r"):
                drivetrain.turn(-float(cmd[2]), 0.75)
            elif (cmd[1] == "b"):
                drivetrain.straight(-float(cmd[2]), 0.75)
            elif (cmd[1] == "l"):
                drivetrain.turn(float(cmd[2]), 0.75)
            elif (cmd[1] == "a"):
                servo_one.set_angle(float(cmd[2]))
            pestolink._byte_list[0] = 0
        if (currentlyMoving and drivetrain.left_motor.get_speed() <= 1 and drivetrain.right_motor.get_speed() <= 1):
            currentlyMoving = False
            print(f"Finished command {cmd}, ready for next command")
            pestolink.send(b"Ready for next command")
        if not currentlyMoving:
            print("Not moving")
            # pestolink.send(b"Ready for next command")
            time.sleep(0.5)
            
                
    else: #default behavior when no BLE connection is open
        print(f"Not connected! ({time.time()})")
        board.led_off()
        servo_one.set_angle(60)
        drivetrain.arcade(0, 0)
    time.sleep(0.1)
