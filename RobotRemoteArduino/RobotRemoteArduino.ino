#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>
#include <ArduinoJson.h>
#include <EEPROM.h>



//SSID and Password to your ESP Access Point
const char* AP_SSID = "ESP_Remote";
char AP_PASS[20] = "";

const char* secretToken = "";
const char* reqType = "";
const char* reqState = "";
const char* reqNewPass;
int reqNewPassLength;
int eepromAddressCount = 10; //EEPROM Address
int eepromAddress = 20; //EEPROM Address
byte eepromGetValue;
byte eepromGetValueCount;

const int gun1 = 5; //D1
const int gun2 = 4; //D2
const int gun3 = 15; //D8
const int engine1_1 = 16; //D0
const int engine1_2 = 14; //D5
const int engine2_1 = 12; //D6
const int engine2_2 = 13; //D7

ESP8266WebServer server(80); //Server on port 80

void setup(void){
  Serial.begin(9600);
  Serial.println("");
  EEPROM.begin(512);
  pinMode(gun1, OUTPUT);
  pinMode(gun2, OUTPUT);
  pinMode(gun3, OUTPUT);
  pinMode(engine1_1, OUTPUT);
  pinMode(engine1_2, OUTPUT);
  pinMode(engine2_1, OUTPUT);
  pinMode(engine2_2, OUTPUT);
  digitalWrite(gun1, LOW);
  digitalWrite(gun2, LOW);
  digitalWrite(gun3, LOW);
  digitalWrite(engine1_1, LOW);
  digitalWrite(engine1_2, LOW);
  digitalWrite(engine2_1, LOW);
  digitalWrite(engine2_2, LOW);

  (getPassFromEEPROM()).toCharArray(AP_PASS, 50); //get access point password from EEPROM
  WiFi.mode(WIFI_AP); //Only Access point
  WiFi.softAP(AP_SSID, AP_PASS, 5, false, 1); //Start HOTspot removing password will disable security
  
  server.on("/remote", switchRelay);
  server.begin(); //Start server
}

void(* resetFunc) (void) = 0; //declare reset function @ address 0

void loop(void){
  server.handleClient(); //Handle client requests
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
        if(String(reqType).equals("left")){
          server.send(200, "application/json", "{\"result\":\"done\"}");
          reqState = root["state"];
          if(String(reqState).equals("on")){
            digitalWrite(engine1_1, HIGH);
            digitalWrite(engine2_2, HIGH);
          }else if(String(reqState).equals("off")){
            digitalWrite(engine1_1, LOW);
            digitalWrite(engine2_2, LOW);
          }
        }else if(String(reqType).equals("right")){
          server.send(200, "application/json", "{\"result\":\"done\"}");
          reqState = root["state"];
          if(String(reqState).equals("on")){
            digitalWrite(engine1_2, HIGH);
            digitalWrite(engine2_1, HIGH);
          }else if(String(reqState).equals("off")){
            digitalWrite(engine1_2, LOW);
            digitalWrite(engine2_1, LOW);
          }
        }else if(String(reqType).equals("forward")){
          server.send(200, "application/json", "{\"result\":\"done\"}");
          reqState = root["state"];
          if(String(reqState).equals("on")){
            digitalWrite(engine1_1, HIGH);
            digitalWrite(engine2_1, HIGH);
          }else if(String(reqState).equals("off")){
            digitalWrite(engine1_1, LOW);
            digitalWrite(engine2_1, LOW);
          }
        }else if(String(reqType).equals("backward")){
          server.send(200, "application/json", "{\"result\":\"done\"}");
          reqState = root["state"];
          if(String(reqState).equals("on")){
            digitalWrite(engine1_2, HIGH);
            digitalWrite(engine2_2, HIGH);
          }else if(String(reqState).equals("off")){
            digitalWrite(engine1_2, LOW);
            digitalWrite(engine2_2, LOW);
          }
        }else if(String(reqType).equals("gun1")){
          server.send(200, "application/json", "{\"result\":\"done\"}");
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
