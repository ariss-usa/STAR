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
7. Download the latest releases for [GPredict](https://github.com/csete/gpredict), [Direwolf](https://github.com/wb2osz/direwolf), [rtl-sdr](https://ftp.osmocom.org/binaries/windows/rtl-sdr/)
8. Extract these folders and move them to the directory which contains the STAR folder 
9. Set up the Discord component
    1. Navigate to Discord Developer (https://discord.com/developers/applications) and click on 'Create New Application'
    2. Create your own server: click on the green plus sign on the bottom left and enter the required information
    3. Enter your newly created server and create a new channel by pressing the plus sign to the right of 'TEXT CHANNELS', and name it 'Directory'
    4. Add a .env file underneath the STAR directory and add the tokens for the 'General', 'Directory', and the Discord bot as demonstrated below.
    ![image](https://user-images.githubusercontent.com/32817861/234155072-23d33c04-e326-455f-a5e3-d86dfa2e6994.png)


Your project structure should look something like this:\
📦ariss-usa\
 ┣ 📂.vscode\
 ┣ 📂direwolf-x.x.x-413855e_i686\
 ┣ 📂gpredict-win32-x.x.x\
 ┣ 📂rtl-sdr-64bit-xxxxxxxx\
 ┣ 📂STAR
