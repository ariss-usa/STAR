# Space Telerobotics using Amateur Radio (STAR)
STAR is a software application for developing and executing teleoperation tasks on the Makeblock mBot robotic platform. This application provides a complete system for interfacing with the mBot through multiple communication channels, enabling robust local and remote control.

<p align="center">
    <img src="./media/animation1.gif">
</p>

## Key Features
This app provides a convenient and reliable interface to explore different modes of telecommunications

|           Feature             |Description  |
|------------------------------|--------------|
Local Bluetooth control       |Allows users to connect and control robots via Bluetooth directly from the Mission Controller GUI, enabling low-latency commands in close-range environments.
Remote control via internet   |Enables control of robots over the internet using a WebSocket-backed API, allowing for long-range or cloud-mediated communication.
APRS control                  |Sends commands encoded as APRS packets using gen_packets, and decodes received packets using Direwolf. On Windows, audio is looped back via stereo mix; on Linux, APRS is received via RTL-SDR at 144.39 MHz.
Mission building              |Provides a GUI-based mission planner for constructing command sequences interactively using a dedicated builder window.
Satellite tracking            |	Integrates with gpredict to show live satellite passes, allowing the app to align communications with overhead satellites in real time.
Mars simulator                | Simulates a Mars terrain and mission building interface to support users without hardware.

## Ideal for
- **Educators and Students** who need a reliable, all-in-one solution for teaching robotics and telecommunications concepts with the mBot.
- **Robotics Hobbyists** working with the Makeblock mBot who require a powerful framework for local and remote operation.
- **Amateur Radio Enthusiasts** looking to interface robotic hardware with the APRS network.
- **Developers** who want a complete and extensible software kit to build upon for advanced tele-robotics projects.

##  Available Communication Interfaces
The STAR framework includes the following built-in communication methods:

- **Local**: Direct control via USB/Serial connection.
- **Remote**: Internet-based control via a WebSocket connection to a central server.
- **APRS**: Command and control using the Automatic Packet Reporting System (APRS) over amateur radio.

## Installation
1. Download the latest release for your platform (Windows/Linux).
2. Unzip the downloaded package to a directory of your choice.

## Usage
1. Open the unzipped folder.
2. Launch the application using the appropriate launcher file:
   - On **Windows**: double-click `launch-windows.bat`
   - On **Linux**: run `./launch-linux.sh` from the terminal.

> [!NOTE]  
> If you have downloaded the `lite` package, make sure any required dependencies (e.g., `Direwolf`, `gen_packets`, `rtl_fm`, `gpredict`) are installed and configured according to the platform you’re using.

To control your mBot using this app, you simply need to plug the Bluetooth dongle into the device running the application, and turn on the mBot. The mBot's flashing blue light should turn to a solid blue light to indicate successful pairing. Click the pair dropdown &rarr; Select the Bluetooth dongle's COM port &rarr; click pair. To send commands, select a robot (select the COM port for local control) and enter power, direction, and time values, then hit the send button.

To access remote mBots via the internet, enter your information in the config page, which can be accessed from the setup menu item. After this is done, remote robots will populate the left hand side dropdown. 
> [!tip]
> If controlling a remote mbot, ensure the receiver has turned their do not disturb setting turned off.

The default behaviour for receiving APRS is different on Windows and Linux.

- Windows
    - 
    - There is no direct software pipeline between `rtl_fm` and **Direwolf**.
    - To receive packets, you must either:
        - Use virtual audio cable or stereo mix to route audio output into **Direwolf**, or
        - Place the radio's speaker near your computer's microphone (less reliable)
- Linux
    -
    - APRS audio is piped directly from `rtl_fm` into **Direwolf** using `snd-aloop` kernel module
    - No physical audio routing or sound mixer/ configuration is needed, its handled entirely in software

---
## Installing From Source
1. Download or clone this repository
2. Open this project in an IDE
3. Create a virtual environment inside the root directory
    1. [How do I set up my virtual environment?](https://gist.github.com/MichaelCurrin/3a4d14ba1763b4d6a1884f56a01412b7)
5. Then enter `pip install -r requirements.txt` into the command line to install all modules
6. [Install the drivers for the SDR dongle](https://www.rtl-sdr.com/rtl-sdr-quick-start-guide/)
7. Install JDK 11 or higher and Maven. This project uses JavaFX via Maven (`org.openjfx`), so JavaFX is downloaded automatically at build time. On Linux, ensure JavaFX native libraries are available or use Liberica JDK Full.
8. Download the latest releases for [GPredict](https://github.com/csete/gpredict), [Direwolf](https://github.com/wb2osz/direwolf), [rtl-sdr](https://ftp.osmocom.org/binaries/windows/rtl-sdr/)
9. **WINDOWS ONLY**: Extract these folders and move them to the directory which contains the STAR folder
Your project structure should look something like this:\
📦ariss-usa\
 ┣ 📂direwolf-x.x.x-413855e_i686\
 ┣ 📂gpredict-win32-x.x.x\
 ┣ 📂rtl-sdr-64bit-xxxxxxxx\
 ┣ 📂STAR

