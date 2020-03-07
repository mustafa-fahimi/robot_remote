#include <ESP8266WiFi.h>
#include <WebSocketsServer.h>
#include <ArduinoJson.h>
#include <EEPROM.h>
#include <stdlib.h>

WebSocketsServer webSocket = WebSocketsServer(80);

//variables decleration
WiFiServer wifiServer(8888);
const char* AP_SSID = "ESP_Remote";
char AP_PASS[20] = "asakrobatic";
const char* secretToken = "";
const char* reqType = "";
const char* reqState = "";
const char* reqStrength = "";
const char* reqStrengthRev = "";
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
const unsigned long periodSocketDis = 1000;
int timeCounter = 0;
int timeCounterSocket = 0;
float myVoltFloat = 0.00;
char* myVolt = "";
int motorStrength;
int motorStrengthRev;
char tempCharSocketRes;
char socketResult[10];
String dataToSend = "";
char charDataToSend[50];
WiFiClient client;
DynamicJsonDocument jsonDocRecv(120);
DynamicJsonDocument jsonDocSend(120);
DeserializationError error;

//Pins decleration
//TODO: Use #define instead
const int gun1 = 5; //D1
const int gun2 = 4; //D2
const int gun3 = 15; //D8
const int engine1_1 = 16; //D0
const int engine1_2 = 14; //D5
const int engine2_1 = 12; //D6
const int engine2_2 = 13; //D7
const int pushButton = 3; //RX

// Called when receiving any WebSocket message
void onWebSocketEvent(uint8_t num,
                      WStype_t type,
                      uint8_t * payload,
                      size_t length) {
 
  // Figure out the type of WebSocket event
  switch(type) {
 
    // Client has disconnected
    case WStype_DISCONNECTED:
      Serial.printf("[%u] Disconnected!\n", num);
      break;
 
    // New client has connected
    case WStype_CONNECTED:
      {
        IPAddress ip = webSocket.remoteIP(num);
        Serial.printf("[%u] Connection from ", num);
        Serial.println(ip.toString());
      }
      break;
 
    // Echo text message back to client
    case WStype_TEXT:
      sprintf(socketResult, "%s", payload);
      processJson();
      webSocket.sendTXT(num, dataToSend);
      break;
 
    // For everything else: do nothing
    case WStype_BIN:
    case WStype_ERROR:
    case WStype_FRAGMENT_TEXT_START:
    case WStype_FRAGMENT_BIN_START:
    case WStype_FRAGMENT:
    case WStype_FRAGMENT_FIN:
    default:
      break;
  }
}

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
  // Start WebSocket server and assign callback
  webSocket.begin();
  webSocket.onEvent(onWebSocketEvent);
}

void(* resetFunc) (void) = 0; //declare reset function @ address 0

void loop(void){
  // Look for and handle WebSocket data
  webSocket.loop();
  readResetBtn();
}

void processJson(){
  jsonDocRecv.clear();
  error = deserializeJson(jsonDocRecv, (String)socketResult);
  if(error){
    sendStateToAndroid("fail");
    return;
  }
  secretToken = jsonDocRecv["token"];
  reqType = jsonDocRecv["androidReq"];
  reqState = jsonDocRecv["state"];
  reqNewPass = jsonDocRecv["newPassword"];
  if(((String)secretToken).equals("MatarataSecretToken1994")){
    if(((String)reqType).equals("gun1")){
      myVoltFloat = ((analogRead(A0) * 3.3) / 1023.0) / 0.12;
      myVoltFloat = roundf(myVoltFloat*100.0)/100.0;
      digitalWrite(gun1, HIGH);
      delay(10);
      digitalWrite(gun1, LOW);
      sendDataToAndroid("done", myVoltFloat);
    }else if(((String)reqType).equals("gun2")){
      if(((String)reqState).equals("on")){
        digitalWrite(gun2, HIGH);
        sendStateToAndroid("done");
      }else{
        digitalWrite(gun2, LOW);
        sendStateToAndroid("done");
      }
    }else if(((String)reqType).equals("gun3")){
      if(((String)reqState).equals("on")){
        digitalWrite(gun3, HIGH);
        sendStateToAndroid("done");
      }else{
        digitalWrite(gun3, LOW);
        sendStateToAndroid("done");
      }
    }else if(String(reqType).equals("changePassword")){
      changePassEEPROM();
      sendStateToAndroid("done");
    }
  }else{
    sendStateToAndroid("fail");
  }
}

void sendStateToAndroid(String state){
  dataToSend = "";
  jsonDocSend.clear();
  jsonDocSend["espResult"] = state;
  serializeJson(jsonDocSend, dataToSend);
}

void sendDataToAndroid(String state, float voltage){
  dataToSend = "";
  jsonDocSend.clear();
  jsonDocSend["espResult"] = state;
  jsonDocSend["voltage"] = voltage;
  serializeJson(jsonDocSend, dataToSend);
}

void changePassEEPROM(){
  reqNewPassLength = strlen(reqNewPass);
  EEPROM.write(eepromAddressCount, reqNewPassLength);
  int x = 0;
  for(int i=eepromAddress; i < (eepromAddress + reqNewPassLength); i++){
    EEPROM.write(i, reqNewPass[x]);
    x++;
  }
  EEPROM.commit();
  delay(1500);
  resetFunc(); //call reset  54
}

//void switchRelay(){
//  //Runs if connected user sended some data
//  if(server.arg(0) != NULL){
//    
//    JsonObject& root = jsonBuffer.parseObject(server.arg(0));
//    if (!root.success()) {
//      server.send(200, "application/json", "{\"result\":\"NodeMcuError\"}");
//    }else if(root.success()){
//      secretToken = root["token"];
//      if(String(secretToken).equals("MatarataSecretToken1994")){
//        reqType = root["request"];
//        reqStrength = root["strength"];
//        reqStrengthRev = root["strengthRev"];
//        motorStrength = ((String)reqStrength).toInt();
//        motorStrengthRev = (( tring)reqStrengthRev).toInt();
//        if(String(reqType).equals("motor_left")){
//          server.send(200, "application/json", "{\"result\":\"done\"}");
//          digitalWrite(engine1_1, HIGH);
//          analogWrite(engine1_2, motorStrengthRev);
//          analogWrite(engine2_1, motorStrengthRev);
//          digitalWrite(engine2_2, HIGH);
//        }else if(String(reqType).equals("motor_right")){
//          server.send(200, "application/json", "{\"result\":\"done\"}");
//          digitalWrite(engine1_1, LOW);
//          analogWrite(engine1_2, motorStrength);
//          analogWrite(engine2_1, motorStrength);
//          digitalWrite(engine2_2, LOW);
//        }else if(String(reqType).equals("motor_forward")){
//          voltage = ((analogRead(A0) * 3.3) / 1023.0) / 0.12;
//          server.send(200, "application/json", "{\"result\":\"done\", \"voltage\":\"" + (String) voltage + "\"}");
//          digitalWrite(engine1_1, HIGH);
//          analogWrite(engine1_2, motorStrengthRev);
//          analogWrite(engine2_1, motorStrength);
//          digitalWrite(engine2_2, LOW);
//        }else if(String(reqType).equals("motor_backward")){
//          server.send(200, "application/json", "{\"result\":\"done\"}");
//          digitalWrite(engine1_1, LOW);
//          analogWrite(engine1_2, motorStrength);
//          analogWrite(engine2_1, motorStrengthRev);
//          digitalWrite(engine2_2, HIGH);
//        }else if(String(reqType).equals("motor_off")){
//          server.send(200, "application/json", "{\"result\":\"done\"}");
//          digitalWrite(engine1_1, LOW);
//          analogWrite(engine1_2, 0);
//          analogWrite(engine2_1, 0);
//          digitalWrite(engine2_2, LOW);
//        }else if(String(reqType).equals("gun1")){
//          voltage = ((analogRead(A0) * 3.3) / 1023.0) / 0.12;
//          server.send(200, "application/json", "{\"result\":\"done\", \"voltage\":\"" + (String) voltage + "\"}");
//          digitalWrite(gun1, HIGH);
//          delay(400);
//          digitalWrite(gun1, LOW);
//        }else if(String(reqType).equals("gun2")){
//          server.send(200, "application/json", "{\"result\":\"done\"}");
//          reqState = root["state"];
//          if(String(reqState).equals("on")){
//            digitalWrite(gun2, HIGH);
//          }else if(String(reqState).equals("off")){
//            digitalWrite(gun2, LOW);
//          }
//        }else if(String(reqType).equals("gun3")){
//          server.send(200, "application/json", "{\"result\":\"done\"}");
//          reqState = root["state"];
//          if(String(reqState).equals("on")){
//            digitalWrite(gun3, HIGH);
//          }else if(String(reqState).equals("off")){
//            digitalWrite(gun3, LOW);
//          }
//        }else if(String(reqType).equals("changePassword")){
//          server.send(200, "application/json", "{\"result\":\"done\"}");
//          reqNewPass = root["newPassword"];
//          reqNewPassLength = strlen(reqNewPass);
//          EEPROM.write(eepromAddressCount, reqNewPassLength);
//          int x = 0;
//          for(int i=eepromAddress; i < (eepromAddress + reqNewPassLength); i++){
//            EEPROM.write(i, reqNewPass[x]);
//            x++;
//          }
//          EEPROM.commit();
//          delay(1500);
//          resetFunc(); //call reset 
//        }else{
//          server.send(200, "application/json", "{\"result\":\"NodeMcuError\"}");
//        }
//      }else{
//        server.send(200, "application/json", "{\"result\":\"WrongToken\"}");
//      }
//    }
//  }
//}

String getPassFromEEPROM(){
  eepromGetValueCount = EEPROM.read(eepromAddressCount);
  String data = "";
  int tempAddress = eepromAddress;
  for(int i=eepromAddress; i < (eepromAddress + eepromGetValueCount); i++){
    data = data + ((char) EEPROM.read(i));
  }
  return data;
}

void readResetBtn(){
  buttonState = digitalRead(pushButton);
  if(buttonState == LOW) {
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
  }else if(buttonState == HIGH){
    timeCounter = 0;
  }
}
