Package via: jpackage --name MissionController --input .\target --main-jar star-1.0-SNAPSHOT-jar-with-dependencies.jar --main-class org.ariss.star.Main --type app-image --dest dist --app-version 1.3

pyinstaller --hidden-import=winpty --hidden-import pywinpty backend.py