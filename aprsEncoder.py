import os

def encode():
    str = f"echo WB2OSZ^^^>WORLD: To KAD3H0 100 forward 2 | gen_packets -a 25 -o x.wav -"
    os.system(str)

    #playsound('./x.wav')
    #os.system('aplay x.wav')
    os.system("atest x.wav >> x.txt")

    with open("x.txt", "r+") as f:
        str = f.read()
        f.truncate(0)
        return str