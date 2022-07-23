#include<SoftwareSerial.h>
#include <MeMCore.h>

MeDCMotor leftWheel(M1);
MeDCMotor rightWheel(M2);

String str="";
void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  while (!Serial) {
    ; // wait for serial port to connect. Needed for native USB port only
  }
  establishContact();  // send a byte to establish contact until receiver responds
}

void loop() {
    if (Serial.available() > 0) {
      // get incoming byte:
      str = Serial.readString();

      // 100 forward 12
      Serial.println(str);

      int ind = str.indexOf(" ");
      int power = str.substring(0, ind).toInt();
      int ind2 = str.indexOf(" ", ind + 1);
      String direc = str.substring(ind + 1, ind2);
      int t = str.substring(ind2 + 1, str.length()).toInt() * 1000;
      if(direc.equals("left")){
        left(power);
      }
      else if(direc.equals("forward")){
        forward(power);
      }
      else if(direc.equals("right")){
        right(power);
      }
      else{
        backward(power);
      }
      delay(t);
      leftWheel.stop();
      rightWheel.stop();
    }
}
void left(int power){
  leftWheel.run(power);
  rightWheel.run(power);
}
void right(int power){
  leftWheel.run(-power);
  rightWheel.run(-power);
}
void forward(int power){
  leftWheel.run(-power);
  rightWheel.run(power);
}
void backward(int power){
  leftWheel.run(power);
  rightWheel.run(-power);
}
void establishContact() {
  while (Serial.available() <= 0) {
    Serial.println("0");   // send an initial string
    delay(300);
  }
}