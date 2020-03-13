#include <Arduino.h>          //library we need in general in VS Code IDE
#include <ESP8266WiFi.h>      //library for wifi access point
#include <WebSocketsServer.h> //libarry for web sockets with event handler
#include <ArduinoJson.h>      //library for parsing json
#include <EEPROM.h>           //libarry for working with EEPROM

//variables declaration
const char *AP_SSID = "Asakrobo_Remote";           //store name of wifi access point
const char *secretToken = "";                      //store token we get from android for security
const char *reqType = "";                          //store request type we get from android
const char *reqState = "";                         //store request state we get from android
const char *reqStrength = "";                      //store strength we get from android
const char *reqNewPass = "";                       //store new password we get from android
char AP_PASS[30] = "asakrobatic";                  //store password of wifi access point
char socketResult[200];                            //store whole json result we get from android
String eepromGetValue;                             //store value we get from EEPROM
byte eepromGetValueCount;                          //store value counting for EEPROM read
unsigned long startMillis;                         //Store time in milisecond for reset button
unsigned long startMillisSocket;                   //Store time in milisecond for socket
unsigned long currentMillis;                       //store current time for reset button
unsigned long currentMillisSocket;                 //store current time for socket
unsigned long period = 500;                        //store a value as period for using in reset button and socket auto disconnect
int reqNewPassLength;                              //store length of new password we get from android
int eepromAddressCount = 10;                       //EEPROM Address for storing password length
int eepromAddress = 25;                            //EEPROM Address for storing password
int buttonState = 0;                               //store the state of reset button
int timeCounter = 0;                               //store amount of time elapssed for reset button
int timeCounterSocket = 0;                         //store amount of time elapssed for socket
int motorStrength = 0;                             //store int value of strength we get from android
uint8_t globalSocketNum;                           //store the socket client number for public use
String dataToSend = "";                            //store json data we want to send to android as string
float myVoltFloat = 0.00;                          //store voltage as float to send to android
DynamicJsonDocument jsonDocRecv(200);              //json document for json we recieve
DynamicJsonDocument jsonDocSend(200);              //json document for json we send
DeserializationError error;                        //store error in deserialization of json
WebSocketsServer webSocket = WebSocketsServer(80); //webSocket variable and his port
WiFiEventHandler stationDisconnectedHandler;       //event handler for wifi access point

//Pins declaration
const int gun1 = 5;            //first relay
const int gun2 = 4;            //second relay
const int gun3 = 15;           //third relay
const int engine1_1 = 16;      //engine one, first pin
const int engine1_2 = 13;      //enigne one, second pin
const int engine1_enable = 14; //engine one, enable pin
const int engine2_1 = 1;       //engine two, first pin
const int engine2_2 = 2;       //engine two, second pin
const int engine2_enable = 12; //engine two, enable pin
const int pushButton = 3;      //reset button

//Methods declaration
void onWebSocketEvent(uint8_t num, WStype_t type, uint8_t *payload, size_t length); //event handler for webSocket
void onStationDisconnected(const WiFiEventSoftAPModeStationDisconnected &evt);      //event handler for wifi access point
void processJson();                                                                 //handle json data we get from android and do the proper job
void sendStateToAndroid(String state);                                              //send only a state like "done" or "fail" to android
void sendDataToAndroid(String state, float voltage);                                //send state and voltage to android
void changePassEEPROM();                                                            //change the password in EEPROM
void readResetBtn();                                                                //read the state of reset button and do factory reset if it is holded 3 second
void autoSocketDisconnect();                                                        //automatically disconnect the socket if we get no data for 3 second
String getPassFromEEPROM();                                                         //get wifi access point password from EEPROM

void onWebSocketEvent(uint8_t num,
                      WStype_t type,
                      uint8_t *payload,
                      size_t length)
{
  // Figure out the type of WebSocket event
  switch (type)
  {
  case WStype_TEXT:
    timeCounterSocket = 0;
    autoSocketDisconnect();
    globalSocketNum = num;
    sprintf(socketResult, "%s", payload);
    processJson();
    break;
  case WStype_DISCONNECTED:
    digitalWrite(gun1, LOW);
    digitalWrite(gun2, LOW);
    digitalWrite(gun3, LOW);
    digitalWrite(engine1_1, LOW);
    digitalWrite(engine1_2, LOW);
    analogWrite(engine1_enable, 0);
    digitalWrite(engine2_1, LOW);
    digitalWrite(engine2_2, LOW);
    analogWrite(engine2_enable, 0);
  case WStype_CONNECTED:
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

void setup(void)
{
  Serial.begin(115200);
  EEPROM.begin(512);
  startMillis = millis();
  startMillisSocket = millis();

  pinMode(gun1, OUTPUT);
  pinMode(gun2, OUTPUT);
  pinMode(gun3, OUTPUT);
  pinMode(engine1_1, OUTPUT);
  pinMode(engine1_2, OUTPUT);
  pinMode(engine1_enable, OUTPUT);
  //pinMode(engine2_1, OUTPUT);
  //pinMode(engine2_2, OUTPUT);
  pinMode(engine2_enable, OUTPUT);
  pinMode(pushButton, INPUT_PULLUP);
  pinMode(A0, INPUT);

  digitalWrite(gun1, LOW);
  digitalWrite(gun2, LOW);
  digitalWrite(gun3, LOW);
  digitalWrite(engine1_1, LOW);
  digitalWrite(engine1_2, LOW);
  analogWrite(engine1_enable, 0);
  //digitalWrite(engine2_1, LOW);
  //digitalWrite(engine2_2, LOW);
  analogWrite(engine2_enable, 0);

  (getPassFromEEPROM()).toCharArray(AP_PASS, 30); //get access point password from EEPROM
  WiFi.persistent(false);
  WiFi.mode(WIFI_AP);
  WiFi.softAP(AP_SSID, AP_PASS, 5, false, 1); //Start Hotspot removing password will disable security
  stationDisconnectedHandler = WiFi.onSoftAPModeStationDisconnected(&onStationDisconnected);
  // Start WebSocket server and assign callback
  webSocket.begin();
  webSocket.onEvent(onWebSocketEvent);
}

void loop(void)
{
  // Look for and handle WebSocket data
  webSocket.loop();
  readResetBtn();
  delay(10);
}

void processJson()
{
  autoSocketDisconnect();
  jsonDocRecv.clear();
  error = deserializeJson(jsonDocRecv, (String)socketResult);
  if (error)
  {
    sendStateToAndroid("fail");
    return;
  }
  secretToken = jsonDocRecv["token"];
  reqType = jsonDocRecv["androidReq"];
  if (((String)secretToken).equals("MataSecToken"))
  {
    if (((String)reqType).equals("motor_left"))
    {
      reqStrength = jsonDocRecv["strength"];
      motorStrength = ((String)reqStrength).toInt();
      digitalWrite(engine1_1, LOW);
      digitalWrite(engine1_2, HIGH);
      digitalWrite(engine2_1, HIGH);
      digitalWrite(engine2_2, LOW);
      analogWrite(engine1_enable, motorStrength);
      analogWrite(engine2_enable, motorStrength);
    }
    else if (((String)reqType).equals("motor_right"))
    {
      reqStrength = jsonDocRecv["strength"];
      motorStrength = ((String)reqStrength).toInt();
      digitalWrite(engine1_1, HIGH);
      digitalWrite(engine1_2, LOW);
      digitalWrite(engine2_1, LOW);
      digitalWrite(engine2_2, HIGH);
      analogWrite(engine1_enable, motorStrength);
      analogWrite(engine2_enable, motorStrength);
    }
    else if (((String)reqType).equals("motor_forward"))
    {
      reqStrength = jsonDocRecv["strength"];
      motorStrength = ((String)reqStrength).toInt();
      digitalWrite(engine1_1, HIGH);
      digitalWrite(engine1_2, LOW);
      digitalWrite(engine2_1, HIGH);
      digitalWrite(engine2_2, LOW);
      analogWrite(engine1_enable, motorStrength);
      analogWrite(engine2_enable, motorStrength);
    }
    else if (((String)reqType).equals("motor_backward"))
    {
      reqStrength = jsonDocRecv["strength"];
      motorStrength = ((String)reqStrength).toInt();
      digitalWrite(engine1_1, LOW);
      digitalWrite(engine1_2, HIGH);
      digitalWrite(engine2_1, LOW);
      digitalWrite(engine2_2, HIGH);
      analogWrite(engine1_enable, motorStrength);
      analogWrite(engine2_enable, motorStrength);
    }
    else if (((String)reqType).equals("motor_off"))
    {
      myVoltFloat = ((analogRead(A0) * 3.3) / 1023.0) / 0.12;
      myVoltFloat = roundf(myVoltFloat * 100.0) / 100.0;
      sendDataToAndroid("done", myVoltFloat);
      digitalWrite(engine1_1, LOW);
      digitalWrite(engine1_2, LOW);
      digitalWrite(engine2_1, LOW);
      digitalWrite(engine2_2, LOW);
      analogWrite(engine1_enable, 0);
      analogWrite(engine2_enable, 0);
    }
    else if (((String)reqType).equals("gun1"))
    {
      digitalWrite(gun1, HIGH);
      delay(10);
      digitalWrite(gun1, LOW);
    }
    else if (((String)reqType).equals("gun2"))
    {
      reqState = jsonDocRecv["state"];
      myVoltFloat = ((analogRead(A0) * 3.3) / 1023.0) / 0.12;
      myVoltFloat = roundf(myVoltFloat * 100.0) / 100.0;
      if (((String)reqState).equals("on"))
      {
        digitalWrite(gun2, HIGH);
        sendDataToAndroid("done", myVoltFloat);
      }
      else
      {
        digitalWrite(gun2, LOW);
      }
    }
    else if (((String)reqType).equals("gun3"))
    {
      reqState = jsonDocRecv["state"];
      myVoltFloat = ((analogRead(A0) * 3.3) / 1023.0) / 0.12;
      myVoltFloat = roundf(myVoltFloat * 100.0) / 100.0;
      if (((String)reqState).equals("on"))
      {
        digitalWrite(gun3, HIGH);
        sendDataToAndroid("done", myVoltFloat);
      }
      else
      {
        digitalWrite(gun3, LOW);
        sendStateToAndroid("done");
      }
    }
    else if (String(reqType).equals("changePassword"))
    {
      reqNewPass = jsonDocRecv["newPassword"];
      changePassEEPROM();
    }
  }
  else
  {
    sendStateToAndroid("fail");
  }
}

void sendStateToAndroid(String state)
{
  dataToSend = "";
  jsonDocSend.clear();
  jsonDocSend["espResult"] = state;
  serializeJson(jsonDocSend, dataToSend);
  webSocket.sendTXT(globalSocketNum, dataToSend);
}

void sendDataToAndroid(String state, float voltage)
{
  dataToSend = "";
  jsonDocSend.clear();
  jsonDocSend["espResult"] = state;
  jsonDocSend["voltage"] = voltage;
  serializeJson(jsonDocSend, dataToSend);
  webSocket.sendTXT(globalSocketNum, dataToSend);
}

void onStationDisconnected(const WiFiEventSoftAPModeStationDisconnected &evt)
{
  digitalWrite(gun1, LOW);
  digitalWrite(gun2, LOW);
  digitalWrite(gun3, LOW);
  digitalWrite(engine1_1, LOW);
  digitalWrite(engine1_2, LOW);
  analogWrite(engine1_enable, 0);
  digitalWrite(engine2_1, LOW);
  digitalWrite(engine2_2, LOW);
  analogWrite(engine2_enable, 0);
  webSocket.disconnect(globalSocketNum);
}

void changePassEEPROM()
{
  reqNewPassLength = strlen(reqNewPass);
  EEPROM.write(eepromAddressCount, reqNewPassLength);
  int x = 0;
  for (int i = eepromAddress; i < (eepromAddress + reqNewPassLength); i++)
  {
    EEPROM.write(i, reqNewPass[x]);
    x++;
  }
  bool a = EEPROM.commit();
  if (a == 1)
  {
    sendStateToAndroid("done");
  }
  else
  {
    sendStateToAndroid("fail");
  }
  delay(800);
  ESP.restart();
}

String getPassFromEEPROM()
{
  eepromGetValueCount = EEPROM.read(eepromAddressCount);
  eepromGetValue = "";
  for (int i = eepromAddress; i < (eepromAddress + eepromGetValueCount); i++)
  {
    eepromGetValue = eepromGetValue + ((char)EEPROM.read(i));
  }
  return eepromGetValue;
}

void readResetBtn()
{
  buttonState = digitalRead(pushButton);
  if (buttonState == LOW)
  {
    currentMillis = millis();
    if (currentMillis - startMillis >= period)
    {
      startMillis = currentMillis;
      timeCounter++;
    }
    if (timeCounter == 6)
    {
      timeCounter = 0;
      char defaultPass[12] = "asakrobatic";
      EEPROM.write(eepromAddressCount, 11);
      int x = 0;
      for (int i = eepromAddress; i < (eepromAddress + 11); i++)
      {
        EEPROM.write(i, defaultPass[x]);
        x++;
      }
      EEPROM.commit();
      Serial.println("resseting");
      delay(1000);
      ESP.restart();
    }
  }
  else if (buttonState == HIGH)
  {
    timeCounter = 0;
  }
}

void autoSocketDisconnect()
{
  currentMillisSocket = millis();
  if (currentMillisSocket - startMillisSocket >= period)
  {
    startMillisSocket = currentMillisSocket;
    timeCounterSocket++;
  }
  if (timeCounterSocket == 6)
  {
    timeCounterSocket = 0;
    webSocket.disconnect();
  }
}