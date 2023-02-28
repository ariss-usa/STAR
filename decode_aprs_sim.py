import time
import aprsEncoder

for i in range(0, 5):
    string = aprsEncoder.encode()
    print(string)
    time.sleep(5)