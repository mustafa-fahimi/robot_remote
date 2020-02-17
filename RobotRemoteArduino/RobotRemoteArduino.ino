#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>
#include <ArduinoJson.h>

//SSID and Password to your ESP Access Point
const char* ssid = "ESPWebServer";
const char* password = "12345678";

const char* secretToken = "";
const char* reqType = "";
const char* reqState = "";
const char* reqNewPass = "";

const int gun1 = 5;
const int gun2 = 4;
const int gun3 = 15;
const int engine1_1 = 16;
const int engine1_2 = 14;
const int engine2_1 = 12;
const int engine2_2 = 13;

ESP8266WebServer server(80); //Server on port 80

void setup(void){
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
  WiFi.mode(WIFI_AP); //Only Access point
  WiFi.softAP(ssid, password,5,false,1); //Start HOTspot removing password will disable security
  
  server.on("/remote", switchRelay);
  server.begin(); //Start server
}

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
          Serial.println("left");
          reqState = root["state"];
          if(String(reqState).equals("on")){
            digitalWrite(engine1_1, HIGH);
            digitalWrite(engine2_2, HIGH);
            server.send(200, "application/json", "{\"result\":\"done\"}");
          }else if(String(reqState).equals("off")){
            digitalWrite(engine1_1, LOW);
            digitalWrite(engine2_2, LOW);
            server.send(200, "application/json", "{\"result\":\"done\"}");
          }
        }else if(String(reqType).equals("right")){
          reqState = root["state"];
          if(String(reqState).equals("on")){
            digitalWrite(engine1_2, HIGH);
            digitalWrite(engine2_1, HIGH);
            server.send(200, "application/json", "{\"result\":\"done\"}");
          }else if(String(reqState).equals("off")){
            digitalWrite(engine1_2, LOW);
            digitalWrite(engine2_1, LOW);
            server.send(200, "application/json", "{\"result\":\"done\"}");
          }
        }else if(String(reqType).equals("forward")){
          reqState = root["state"];
          if(String(reqState).equals("on")){
            digitalWrite(engine1_1, HIGH);
            digitalWrite(engine2_1, HIGH);
            server.send(200, "application/json", "{\"result\":\"done\"}");
          }else if(String(reqState).equals("off")){
            digitalWrite(engine1_1, LOW);
            digitalWrite(engine2_1, LOW);
            server.send(200, "application/json", "{\"result\":\"done\"}");
          }
        }else if(String(reqType).equals("backward")){
          reqState = root["state"];
          if(String(reqState).equals("on")){
            digitalWrite(engine1_2, HIGH);
            digitalWrite(engine2_2, HIGH);
            server.send(200, "application/json", "{\"result\":\"done\"}");
          }else if(String(reqState).equals("off")){
            digitalWrite(engine1_2, LOW);
            digitalWrite(engine2_2, LOW);
            server.send(200, "application/json", "{\"result\":\"done\"}");
          }
        }else if(String(reqType).equals("gun1")){
          digitalWrite(gun1, HIGH);
          delay(400);
          digitalWrite(gun1, LOW);
          server.send(200, "application/json", "{\"result\":\"done\"}");
        }else if(String(reqType).equals("gun2")){
          reqState = root["state"];
          if(String(reqState).equals("on")){
            digitalWrite(gun2, HIGH);
            server.send(200, "application/json", "{\"result\":\"done\"}");
          }else if(String(reqState).equals("off")){
            digitalWrite(gun2, LOW);
            server.send(200, "application/json", "{\"result\":\"done\"}");
          }
        }else if(String(reqType).equals("gun3")){
          reqState = root["state"];
          if(String(reqState).equals("on")){
            digitalWrite(gun3, HIGH);
            server.send(200, "application/json", "{\"result\":\"done\"}");
          }else if(String(reqState).equals("off")){
            digitalWrite(gun3, LOW);
            server.send(200, "application/json", "{\"result\":\"done\"}");
          }
        }else if(String(reqType).equals("changePassword")){
          //TODO: Set new password to eeprom
          reqNewPass = root["newPassword"];
          
        }else{
          server.send(200, "application/json", "{\"result\":\"NodeMcuError\"}");
        }
      }else{
        server.send(200, "application/json", "{\"result\":\"WrongToken\"}");
      }
    }
  }
}
