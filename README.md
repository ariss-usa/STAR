# STAR
The ARISS STAR Project code and docs are contained within this repo.
The STAR project allows for an easy way for a user to control the Makeblock mBot remotely and locally through a simple user interface.

## Purpose ##
The purpose of this project is to prepare and familiarize students with remote communications before their contact with the International Space Station. 
[Learn more about ARISS!](https://www.ariss.org/)

## Use ##
If you would like to control your own mBot locally, you must follow the instructions in the firmware folder; then, complete the installation instructions below.
If you don't have your own mBot or you only want to control remote mBots, skip the firmware folder instructions and just follow the installation instructions below.

## Installation (FOR DEVELOPERS) ##
1. Download or clone this repository
2. Open this project in Visual Studio Code
3. Create a virtual environment inside the root directory
    1. [How do I set up my virtual environment?](https://gist.github.com/MichaelCurrin/3a4d14ba1763b4d6a1884f56a01412b7)
5. Then enter `pip install -r requirements.txt` into the command line to install all modules
6. [Install the drivers for the SDR dongle](https://www.rtl-sdr.com/rtl-sdr-quick-start-guide/)
7. Download [GPredict](https://github.com/csete/gpredict), [Direwolf](https://github.com/wb2osz/direwolf), [rtl-sdr](https://ftp.osmocom.org/binaries/windows/rtl-sdr/)
8. Obtain API tokens before use
