#include <ESP8266WiFi.h>
#include <WebSocketsServer.h>
#include <ArduinoJson.h>
#include <EEPROM.h>
#include <stdlib.h>

#define gun1 5
#define gun2 4
#define gun3 15
#define engine1_1 11
#define engine1_2 7
#define engine1_enable 14
#define engine2_1 6
#define engine2_2 8
#define engine2_enable 12
#define pushButton 3

WebSocketsServer webSocket = WebSocketsServer(80);

//variables decleration
const char* AP_SSID = "ESP_Remote";
const char* secretToken = "";
const char* reqType = "";
const char* reqState = "";
const char* reqStrength = "";
const char* reqStrengthRev = "";
const char* reqNewPass = "";
char* AP_PASS;
char* socketResult;
String dataToSend = "";
float myVoltFloat = 0.00;
byte eepromGetValue;
byte eepromGetValueCount;
unsigned long startMillis; 
unsigned long currentMillis;
unsigned long currentMillisSocket;
unsigned long period = 500;
int reqNewPassLength;
int eepromAddressCount = 10; //EEPROM Address for storing password length
int eepromAddress = 25; //EEPROM Address for storing password
int buttonState = 0;
int timeCounter = 0;
int timeCounterSocket = 0;
int motorStrength = 0;
int motorStrengthRev = 0;
uint8_t globalSocketNum;
DynamicJsonDocument jsonDocRecv(250);
DynamicJsonDocument jsonDocSend(250);
DeserializationError error;

//Pins decleration
//const int gun1 = 5; //D1
//const int gun2 = 4; //D2
//const int gun3 = 15; //D8
//const int engine1_1 = 16; //D0
//const int engine1_2 = 14; //D5
//const int engine2_1 = 12; //D6
//const int engine2_2 = 13; //D7
//const int pushButton = 3; //RX

// Called when receiving any WebSocket message
void onWebSocketEvent(uint8_t num,
                      WStype_t type,
                      uint8_t * payload,
                      size_t length) {
 
  // Figure out the type of WebSocket event
  switch(type) {
    case WStype_TEXT:
      timeCounterSocket = 0;
      globalSocketNum = num;
      sprintf(socketResult, "%s", payload);
      processJson();
      break;
    case WStype_CONNECTED:
    case WStype_DISCONNECTED:
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
  autoSocketDisconnect();
  jsonDocRecv.clear();
  error = deserializeJson(jsonDocRecv, (String)socketResult);
  if(error){
    sendStateToAndroid("fail");
    return;
  }
  secretToken = jsonDocRecv["token"];
  reqType = jsonDocRecv["androidReq"];
  if(((String)secretToken).equals("MatarataSecretToken1994")){
    if(((String)reqType).equals("motor_left")){
      reqStrengthRev = jsonDocRecv["strengthRev"];
      motorStrengthRev = ((String)reqStrengthRev).toInt();
      digitalWrite(engine1_1, HIGH);
      analogWrite(engine1_2, motorStrengthRev);
      analogWrite(engine2_1, motorStrengthRev);
      digitalWrite(engine2_2, HIGH);
    }else if(((String)reqType).equals("motor_right")){
      reqStrength = jsonDocRecv["strength"];
      motorStrength = ((String)reqStrength).toInt();
      digitalWrite(engine1_1, LOW);
      analogWrite(engine1_2, motorStrength);
      analogWrite(engine2_1, motorStrength);
      digitalWrite(engine2_2, LOW);
    }else if(((String)reqType).equals("motor_forward")){
      reqStrength = jsonDocRecv["strength"];
      reqStrengthRev = jsonDocRecv["strengthRev"];
      motorStrength = ((String)reqStrength).toInt();
      motorStrengthRev = ((String)reqStrengthRev).toInt();
      digitalWrite(engine1_1, HIGH);
      analogWrite(engine1_2, motorStrengthRev);
      analogWrite(engine2_1, motorStrength);
      digitalWrite(engine2_2, LOW);
    }else if(((String)reqType).equals("motor_backward")){
      reqStrength = jsonDocRecv["strength"];
      reqStrengthRev = jsonDocRecv["strengthRev"];
      motorStrength = ((String)reqStrength).toInt();
      motorStrengthRev = ((String)reqStrengthRev).toInt();
      digitalWrite(engine1_1, LOW);
      analogWrite(engine1_2, motorStrength);
      analogWrite(engine2_1, motorStrengthRev);
      digitalWrite(engine2_2, HIGH);
    }else if(((String)reqType).equals("motor_off")){
      myVoltFloat = ((analogRead(A0) * 3.3) / 1023.0) / 0.12;
      myVoltFloat = roundf(myVoltFloat*100.0)/100.0;
      sendDataToAndroid("done", myVoltFloat);
      digitalWrite(engine1_1, LOW);
      analogWrite(engine1_2, 0);
      analogWrite(engine2_1, 0);
      digitalWrite(engine2_2, LOW);
    }else if(((String)reqType).equals("gun1")){
      digitalWrite(gun1, HIGH);
      delay(10);
      digitalWrite(gun1, LOW);
    }else if(((String)reqType).equals("gun2")){
      reqState = jsonDocRecv["state"];
      myVoltFloat = ((analogRead(A0) * 3.3) / 1023.0) / 0.12;
      myVoltFloat = roundf(myVoltFloat*100.0)/100.0;
      if(((String)reqState).equals("on")){
        digitalWrite(gun2, HIGH);
        sendDataToAndroid("done", myVoltFloat);
      }else{
        digitalWrite(gun2, LOW);
      }
    }else if(((String)reqType).equals("gun3")){
      reqState = jsonDocRecv["state"];
      myVoltFloat = ((analogRead(A0) * 3.3) / 1023.0) / 0.12;
      myVoltFloat = roundf(myVoltFloat*100.0)/100.0;
      if(((String)reqState).equals("on")){
        digitalWrite(gun3, HIGH);
        sendDataToAndroid("done", myVoltFloat);
      }else{
        digitalWrite(gun3, LOW);
        sendStateToAndroid("done");
      }
    }else if(String(reqType).equals("changePassword")){
      reqNewPass = jsonDocRecv["newPassword"];
      sendStateToAndroid("done");
      changePassEEPROM();
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
  webSocket.sendTXT(globalSocketNum, dataToSend);
}

void sendDataToAndroid(String state, float voltage){
  dataToSend = "";
  jsonDocSend.clear();
  jsonDocSend["espResult"] = state;
  jsonDocSend["voltage"] = voltage;
  serializeJson(jsonDocSend, dataToSend);
  webSocket.sendTXT(globalSocketNum, dataToSend);
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

void autoSocketDisconnect(){
  currentMillisSocket = millis();
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
