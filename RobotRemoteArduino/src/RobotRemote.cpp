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
  case WStype_TEXT:        //for handling incoming texts from android
    timeCounterSocket = 0; //set auto socket disconnect variable to 0 cause we got some data
    autoSocketDisconnect();
    globalSocketNum = num;                //store socket client number to a global variable
    sprintf(socketResult, "%s", payload); //store payload which is the text that we got from android into socketResult variable for global use
    processJson();                        //handle the json data we got from android
    break;
  case WStype_DISCONNECTED: //for handling socket client disconnection
    //set all pins to low cause user disconnected
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

  (getPassFromEEPROM()).toCharArray(AP_PASS, 30);                                            //get access point password from EEPROM and store it in AP_PASS
  WiFi.persistent(false);                                                                    //don't save the wifi state
  WiFi.mode(WIFI_AP);                                                                        //determine the wifi mode for accesss point
  WiFi.softAP(AP_SSID, AP_PASS, 5, false, 1);                                                //start Esp access point and limit it to 1 connection
  stationDisconnectedHandler = WiFi.onSoftAPModeStationDisconnected(&onStationDisconnected); //access point disconnection handler
  // Start WebSocket server and assign callback
  webSocket.begin();
  webSocket.onEvent(onWebSocketEvent);
}

void loop(void)
{
  webSocket.loop(); //loop and handle WebSocket connections
  readResetBtn();   //constantly read button on Esp and if it holded 3 second then do a factory reset
  delay(10);
}

void processJson()
{
  autoSocketDisconnect();
  jsonDocRecv.clear();                                        //clear json recieve document
  error = deserializeJson(jsonDocRecv, (String)socketResult); //deserialize the socketResult that contain json as string into json document
  if (error)
  {
    //if deserialization have error then send fail to android
    sendStateToAndroid("fail");
    return;
  }
  secretToken = jsonDocRecv["token"];  //extract token from json document
  reqType = jsonDocRecv["androidReq"]; //extract androidReq from json document
  if (((String)secretToken).equals("MataSecToken"))
  {
    //here secret token we get from android is correct
    if (((String)reqType).equals("motor_left"))
    {
      //request we get from android is "motor_left"
      reqStrength = jsonDocRecv["strength"];         //extract strength from json document
      motorStrength = ((String)reqStrength).toInt(); //convert strength we got from android to int
      //change pins state for going left
      digitalWrite(engine1_1, LOW);
      digitalWrite(engine1_2, HIGH);
      digitalWrite(engine2_1, HIGH);
      digitalWrite(engine2_2, LOW);
      analogWrite(engine1_enable, motorStrength);
      analogWrite(engine2_enable, motorStrength);
    }
    else if (((String)reqType).equals("motor_right"))
    {
      //request we get from android is "motor_right"
      reqStrength = jsonDocRecv["strength"];         //extract strength from json document
      motorStrength = ((String)reqStrength).toInt(); //convert strength we got from android to int
      //change pins state for going right
      digitalWrite(engine1_1, HIGH);
      digitalWrite(engine1_2, LOW);
      digitalWrite(engine2_1, LOW);
      digitalWrite(engine2_2, HIGH);
      analogWrite(engine1_enable, motorStrength);
      analogWrite(engine2_enable, motorStrength);
    }
    else if (((String)reqType).equals("motor_forward"))
    {
      //request we get from android is "motor_forward"
      reqStrength = jsonDocRecv["strength"];         //extract strength from json document
      motorStrength = ((String)reqStrength).toInt(); //convert strength we got from android to int
      //change pins state for going forward
      digitalWrite(engine1_1, HIGH);
      digitalWrite(engine1_2, LOW);
      digitalWrite(engine2_1, HIGH);
      digitalWrite(engine2_2, LOW);
      analogWrite(engine1_enable, motorStrength);
      analogWrite(engine2_enable, motorStrength);
    }
    else if (((String)reqType).equals("motor_backward"))
    {
      //request we get from android is "motor_backward"
      reqStrength = jsonDocRecv["strength"];         //extract strength from json document
      motorStrength = ((String)reqStrength).toInt(); //convert strength we got from android to int
      //change pins state for going backward
      digitalWrite(engine1_1, LOW);
      digitalWrite(engine1_2, HIGH);
      digitalWrite(engine2_1, LOW);
      digitalWrite(engine2_2, HIGH);
      analogWrite(engine1_enable, motorStrength);
      analogWrite(engine2_enable, motorStrength);
    }
    else if (((String)reqType).equals("motor_off"))
    {
      //request we get from android is "motor_off"
      myVoltFloat = ((analogRead(A0) * 3.3) / 1023.0) / 0.12; //calculate voltage with proper formula
      myVoltFloat = roundf(myVoltFloat * 100.0) / 100.0;      //round voltage to float with 2 decimal
      sendDataToAndroid("done", myVoltFloat);                 //send response to android via voltage
      //change pins state for turning off motors
      digitalWrite(engine1_1, LOW);
      digitalWrite(engine1_2, LOW);
      digitalWrite(engine2_1, LOW);
      digitalWrite(engine2_2, LOW);
      analogWrite(engine1_enable, 0);
      analogWrite(engine2_enable, 0);
    }
    else if (((String)reqType).equals("gun1"))
    {
      //request we get from android is "gun1"
      //change pin state of gun1 for 10 millisecond
      digitalWrite(gun1, HIGH);
      delay(10);
      digitalWrite(gun1, LOW);
    }
    else if (((String)reqType).equals("gun2"))
    {
      //request we get from android is "gun2"
      reqState = jsonDocRecv["state"];                        //extract state from json document
      myVoltFloat = ((analogRead(A0) * 3.3) / 1023.0) / 0.12; //calculate voltage with proper formula
      myVoltFloat = roundf(myVoltFloat * 100.0) / 100.0;      //round voltage to float with 2 decimal
      if (((String)reqState).equals("on"))
      {
        //request state from android is turn on
        digitalWrite(gun2, HIGH);
        sendDataToAndroid("done", myVoltFloat); //send response to android via voltage
      }
      else
      {
        //request state from android is turn off
        digitalWrite(gun2, LOW);
      }
    }
    else if (((String)reqType).equals("gun3"))
    {
      //request we get from android is "gun3"
      reqState = jsonDocRecv["state"];                        //extract state from json document
      myVoltFloat = ((analogRead(A0) * 3.3) / 1023.0) / 0.12; //calculate voltage with proper formula
      myVoltFloat = roundf(myVoltFloat * 100.0) / 100.0;      //round voltage to float with 2 decimal
      if (((String)reqState).equals("on"))
      {
        //request state from android is turn on
        digitalWrite(gun3, HIGH);
        sendDataToAndroid("done", myVoltFloat); //send response to android via voltage
      }
      else
      {
        //request state from android is turn off
        digitalWrite(gun3, LOW);
        sendStateToAndroid("done"); //send response to android
      }
    }
    else if (String(reqType).equals("changePassword"))
    {
      //request we get from android is "change_password"
      reqNewPass = jsonDocRecv["newPassword"]; //extract newPassword from json document
      changePassEEPROM();                      //change wifi access point of Esp in EEPROM and reset Esp
    }
  }
  else
  {
    //token we get from android is wrong so we send fail as response
    sendStateToAndroid("fail");
  }
}

void sendStateToAndroid(String state)
{
  dataToSend = "";                                //empty variable that gonna store data we want to send for avoiding any conflict
  jsonDocSend.clear();                            //clear jseon send document for avoiding any conflict
  jsonDocSend["espResult"] = state;               //add state we get into espResult as json
  serializeJson(jsonDocSend, dataToSend);         //serialize json send document
  webSocket.sendTXT(globalSocketNum, dataToSend); //send json to socket client with correct number
}

void sendDataToAndroid(String state, float voltage)
{
  dataToSend = "";                                //empty variable that gonna store data we want to send for avoiding any conflict
  jsonDocSend.clear();                            //clear jseon send document for avoiding any conflict
  jsonDocSend["espResult"] = state;               //add state we get into espResult as json
  jsonDocSend["voltage"] = voltage;               //add voltage we get into voltage as json
  serializeJson(jsonDocSend, dataToSend);         //serialize json send document
  webSocket.sendTXT(globalSocketNum, dataToSend); //send json to socket client with correct number
}

void onStationDisconnected(const WiFiEventSoftAPModeStationDisconnected &evt)
{
  //our only wifi access point client is disconnected so we set all pins to LOW and disconnect the socket
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
  EEPROM.write(eepromAddressCount, reqNewPassLength); //write length of new password on EEPROM
  int x = 0;
  for (int i = eepromAddress; i < (eepromAddress + reqNewPassLength); i++)
  {
    EEPROM.write(i, reqNewPass[x]);
    x++;
  }
  bool a = EEPROM.commit(); //store result of writing on EEPROM to a boolean
  if (a == 1)
  {
    //we successfully write password on EEPROM
    sendStateToAndroid("done"); //send response to android
  }
  else
  {
    sendStateToAndroid("fail"); //send response to android
  }
  delay(1000);
  ESP.restart(); //restart Esp
}

String getPassFromEEPROM()
{
  eepromGetValueCount = EEPROM.read(eepromAddressCount); //get length of password to from EEPROM
  eepromGetValue = "";
  for (int i = eepromAddress; i < (eepromAddress + eepromGetValueCount); i++)
  {
    eepromGetValue = eepromGetValue + ((char)EEPROM.read(i));
  }
  return eepromGetValue; //return password we read from EEPROM for access point
}

void readResetBtn()
{
  buttonState = digitalRead(pushButton); //read state of reset button
  if (buttonState == LOW)
  {
    //reset button on Esp pushed
    currentMillis = millis();
    if (currentMillis - startMillis >= period)
    {
      //user pushed reset button for half a second cause period is 500 millisecond
      startMillis = currentMillis;
      timeCounter++;
    }
    if (timeCounter == 6)
    {
      //user pushed reset button for 3 seconds (500 * 6 = 3000 millisecond)
      timeCounter = 0;
      char defaultPass[12] = "asakrobatic";
      EEPROM.write(eepromAddressCount, 11);
      int x = 0;
      for (int i = eepromAddress; i < (eepromAddress + 11); i++)
      {
        EEPROM.write(i, defaultPass[x]); //write default password which is "asakrobatic" on EEPROM
        x++;
      }
      EEPROM.commit();
      delay(1000);
      ESP.restart(); //restart Esp
    }
  }
  else if (buttonState == HIGH)
  {
    //reset button on Esp not pushed currently or released
    timeCounter = 0;
  }
}

void autoSocketDisconnect()
{
  currentMillisSocket = millis();
  if (currentMillisSocket - startMillisSocket >= period)
  {
    //we didn't get any data from android for half a second
    startMillisSocket = currentMillisSocket;
    timeCounterSocket++;
  }
  if (timeCounterSocket == 6)
  {
    //we didn't get any data from android for 3 seconds so we set counter to 0 and disconnect socket
    timeCounterSocket = 0;
    webSocket.disconnect();
  }
}