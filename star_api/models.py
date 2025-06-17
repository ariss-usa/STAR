from typing import List
from pydantic import BaseModel

class RobotEntry(BaseModel):
    id: str
    schoolName: str
    city: str
    state: str
    doNotDisturb: bool

class RobotCommand(BaseModel):
    power: float
    direction: str
    time: float

class Command(BaseModel):
    sender_id: str
    receiver_id: str
    commands: List[RobotCommand]