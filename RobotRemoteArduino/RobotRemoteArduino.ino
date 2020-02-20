#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>
#include <ArduinoJson.h>
#include <EEPROM.h>

//variables decleration
const char* AP_SSID = "ESP_Remote";
char AP_PASS[20] = "asakrobatic";
const char* secretToken = "";
const char* reqType = "";
const char* reqState = "";
const char* reqNewPass;
int reqNewPassLength;
int eepromAddressCount = 10; //EEPROM Address
int eepromAddress = 20; //EEPROM Address
byte eepromGetValue;
byte eepromGetValueCount;
int buttonState = 0;
unsigned long startMillis; 
unsigned long currentMillis;
const unsigned long period = 500;
int timeCounter = 0;
float voltage;

//Pins decleration
const int gun1 = 5; //D1
const int gun2 = 4; //D2
const int gun3 = 15; //D8
const int engine1_1 = 16; //D0
const int engine1_2 = 14; //D5
const int engine2_1 = 12; //D6
const int engine2_2 = 13; //D7
const int pushButton = 3; //RX

ESP8266WebServer server(80); //Server on port 80

void setup(void){
  Serial.begin(115200);
  Serial.println("");
  EEPROM.begin(512);
  startMillis = millis();
  
  pinMode(gun1, OUTPUT);
  pinMode(gun2, OUTPUT);
  pinMode(gun3, OUTPUT);
  pinMode(engine1_1, OUTPUT);
  pinMode(engine1_2, OUTPUT);
  pinMode(engine2_1, OUTPUT);
  pinMode(engine2_2, OUTPUT);
  pinMode(pushButton, INPUT_PULLUP);
  pinMode(A0, INPUT);
  
  digitalWrite(gun1, LOW);
  digitalWrite(gun2, LOW);
  digitalWrite(gun3, LOW);
  digitalWrite(engine1_1, LOW);
  digitalWrite(engine1_2, LOW);
  digitalWrite(engine2_1, LOW);
  digitalWrite(engine2_2, LOW);

  (getPassFromEEPROM()).toCharArray(AP_PASS, 20); //get access point password from EEPROM
  WiFi.mode(WIFI_AP);
  WiFi.softAP(AP_SSID, AP_PASS, 5, false, 1); //Start Hotspot removing password will disable security
  
  server.on("/remote", switchRelay);
  server.begin(); //Start server
}

void(* resetFunc) (void) = 0; //declare reset function @ address 0

void loop(void){
  server.handleClient(); //Handle client requests
  buttonState = digitalRead(pushButton);
  if(buttonState == LOW) {
    pushButtonResetHotspot();
  }else if(buttonState == HIGH){
    timeCounter = 0;
  }
}

void switchRelay(){
  //Runs if connected user sended some data
  if(server.arg(0) != NULL){
    DynamicJsonBuffer  jsonBuffer(200);
    JsonObject& root = jsonBuffer.parseObject(server.arg(0));
    if (!root.success()) {
      server.send(200, "application/json", "{\"result\":\"NodeMcuError\"}");
    }else if(root.success()){
      secretToken = root["token"];
      if(String(secretToken).equals("MatarataSecretToken1994")){
        reqType = root["request"];
        if(String(reqType).equals("motor_left")){
          server.send(200, "application/json", "{\"result\":\"done\"}");
          digitalWrite(engine1_1, HIGH);
          digitalWrite(engine2_2, HIGH);
          Serial.println("l");
        }else if(String(reqType).equals("motor_right")){
          server.send(200, "application/json", "{\"result\":\"done\"}");
          digitalWrite(engine1_2, HIGH);
          digitalWrite(engine2_1, HIGH);
          Serial.println("r");
        }else if(String(reqType).equals("motor_forward")){
          voltage = ((analogRead(A0) * 3.3) / 1023.0) / 0.12;
          server.send(200, "application/json", "{\"result\":\"done\", \"voltage\":\"" + (String) voltage + "\"}");
          digitalWrite(engine1_1, HIGH);
          digitalWrite(engine2_1, HIGH);
          Serial.println("f");
        }else if(String(reqType).equals("motor_backward")){
          server.send(200, "application/json", "{\"result\":\"done\"}");
          digitalWrite(engine1_2, HIGH);
          digitalWrite(engine2_2, HIGH);
          Serial.println("b");
        }else if(String(reqType).equals("motor_off")){
          server.send(200, "application/json", "{\"result\":\"done\"}");
          digitalWrite(engine1_1, LOW);
          analogWrite(engine1_2, 0);
          analogWrite(engine2_1, 0);
          digitalWrite(engine2_2, LOW);
          Serial.println("off");
        }else if(String(reqType).equals("gun1")){
          voltage = ((analogRead(A0) * 3.3) / 1023.0) / 0.12;
          server.send(200, "application/json", "{\"result\":\"done\", \"voltage\":\"" + (String) voltage + "\"}");
          digitalWrite(gun1, HIGH);
          delay(400);
          digitalWrite(gun1, LOW);
        }else if(String(reqType).equals("gun2")){
          server.send(200, "application/json", "{\"result\":\"done\"}");
          reqState = root["state"];
          if(String(reqState).equals("on")){
            digitalWrite(gun2, HIGH);
          }else if(String(reqState).equals("off")){
            digitalWrite(gun2, LOW);
          }
        }else if(String(reqType).equals("gun3")){
          server.send(200, "application/json", "{\"result\":\"done\"}");
          reqState = root["state"];
          if(String(reqState).equals("on")){
            digitalWrite(gun3, HIGH);
          }else if(String(reqState).equals("off")){
            digitalWrite(gun3, LOW);
          }
        }else if(String(reqType).equals("changePassword")){
          server.send(200, "application/json", "{\"result\":\"done\"}");
          reqNewPass = root["newPassword"];
          reqNewPassLength = strlen(reqNewPass);
          EEPROM.write(eepromAddressCount, reqNewPassLength);
          int x = 0;
          for(int i=eepromAddress; i < (eepromAddress + reqNewPassLength); i++){
            EEPROM.write(i, reqNewPass[x]);
            x++;
          }
          EEPROM.commit();
          delay(1500);
          //FIXME: gives volley timeout in android
          resetFunc(); //call reset 
        }else{
          server.send(200, "application/json", "{\"result\":\"NodeMcuError\"}");
        }
      }else{
        server.send(200, "application/json", "{\"result\":\"WrongToken\"}");
      }
    }
  }
}

String getPassFromEEPROM(){
  eepromGetValueCount = EEPROM.read(eepromAddressCount);
  String data = "";
  int tempAddress = eepromAddress;
  for(int i=eepromAddress; i < (eepromAddress + eepromGetValueCount); i++){
    data = data + ((char) EEPROM.read(i));
  }
  return data;
}

void pushButtonResetHotspot(){
  currentMillis = millis();
    if(currentMillis - startMillis >= period){
      startMillis = currentMillis;
      timeCounter++;
    }
    if(timeCounter == 6){
      timeCounter = 0;
      char defaultPass[12] = "asakrobatic";
      EEPROM.write(eepromAddressCount, 11);
      int x = 0;
      for(int i=eepromAddress; i < (eepromAddress + 11); i++){
        EEPROM.write(i, defaultPass[x]);
        x++;
      }
      EEPROM.commit();
      delay(1000);
      resetFunc(); //call reset
    }
}
