from enum import Flag


class RobotType(Flag):
    MBOT = False
    XRP = True

cmdQueue = []
robotType = None