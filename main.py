from fastapi import FastAPI
from fastapi import WebSocket, WebSocketDisconnect
from models import RobotEntry
from models import Command

app = FastAPI()

active_robots: dict[str, RobotEntry] = {}
active_connections: dict[str, WebSocket] = {}

@app.get("/robots/active")
def get_active_robots():
    return {
        "status": "ok",
        "active_robots": [robot.model_dump() for robot in active_robots.values()]
    }

@app.post("/robots/update")
async def update(robot: RobotEntry):
    """
    Handle user info changes & push to active users
    """
    if robot.id not in active_robots:
        return {"status": "error"}

    active_robots[robot.id] = robot
    for connection in active_connections.values():
        try:
            await connection.send_json({
                "type": "robot_edit",
                "id": robot.id,
                "data": robot.model_dump()
            })
        except Exception as e:
            print(f"Failed to send to connection: {e}")
    
    return {"status": "ok", "data": robot.model_dump()}

@app.post("/send_command")
async def send_command(command: Command):
    """
    Route sender command
    """

    socket = active_connections.get(command.receiver_id)
    robot = active_robots.get(command.receiver_id)

    if not socket or not robot:
        return {"status": "error", "message": "Receiving robot not online"}
    
    if robot.doNotDisturb:
        return {"status": "error", "message": "Receiving robot is in do not disturb mode"}
    
    await socket.send_json({
        "type": "command",
        "commands": command.commands,
        "sender_id": command.sender_id,
        "receiver_id": command.receiver_id
    })

    return {"status": "ok"}

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    try:
        metadata = await websocket.receive_json() # Client should immediately send their info
        mcID = metadata["id"]
        print(metadata)
        robot = RobotEntry(**metadata) # Make a robot entry

        active_robots[mcID] = robot
        active_connections[mcID] = websocket

        print("CONNECTED")
        while True:
            msg = await websocket.receive_json()

    except WebSocketDisconnect:
        print("Websocket disconnected")
    finally:
        active_connections.pop(mcID, None)
        active_robots.pop(mcID, None)