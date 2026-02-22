from typing import List
from pydantic import BaseModel

class RobotEntry(BaseModel):
    id: str
    schoolName: str
    city: str
    state: str
    doNotDisturb: bool
    robotType: str | None = None

class RobotCommand(BaseModel):
    power: float
    direction: str
    time: float

class XRPCommand(BaseModel):
    direction: str
    amount: float

class Command(BaseModel):
    sender_id: str
    receiver_id: str
    commands: List[RobotCommand | XRPCommand]
    cmdType: str