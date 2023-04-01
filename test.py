from winpty import PtyProcess
import re

proc = PtyProcess.spawn("direwolf -c C:\\Users\\unshr\\ariss-usa-1\\direwolf-1.6.0-413855e_i686\\direwolf-1.6.0-413855e_i686\\direwolf.conf")

while proc.isalive():
    line = proc.readline()
    clean_str = re.sub(r'\x1b\[.*?[@-~]', '', line)
    clean_str = clean_str.strip()
    pattern = re.compile(r'To\s(.*?)\s<0x0a>')
    match = pattern.search(clean_str)
    if match:
        print(match)
    print(line)