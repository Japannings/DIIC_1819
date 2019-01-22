//Libraries

#include <ESP8266WiFi.h> //library to use esp wifi functions
#include <FirebaseArduino.h> //library to use firebase (connect and send/receive data)

// WiFi Credentials
const char* SSID = ""; //network name
const char* PASSWORD = ""; //network password

//Firebase Credentials
#define FIREBASE_HOST ""
#define FIREBASE_AUTH ""

//functions prototypes
void initWiFi();
void recconectWiFi();

//variables declaration
String str = "";
bool activateWatering = false;

void setup(){
  //initiates firebase connection
  Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
  Serial.begin(115200);
  initWiFi(); //call function to connect to wifi
}
void loop(){
    //Checks if there is any data being sent via serial
    if (Serial.available() > 0) {
        //"data" received via serial
        str = Serial.readString();
        
        /*The data was received as a single string, so it needs to be 'treated'.
         * 
         * Each sensor readig is separeted by a bar |
         */
        
        //indexes of each data received
        int Index1 = str.indexOf('|');          //"remove first bar"
        int Index2 = str.indexOf('|', Index1+1);//index of first value - Light
        int Index3 = str.indexOf('|', Index2+1);//index of second value - Temperature
        int Index4 = str.indexOf('|', Index3+1);//index of third value - Water
        int Index5 = str.indexOf('|', Index4+1);//index of fourth value - motion
        int Index6 = str.indexOf('|', Index5+1);//index of fifth value - beingWatered

        //divide incoming string according to indexes
        String sendTemperature = str.substring(Index1+1, Index2);
        String sendLight = str.substring(Index2+1, Index3);
        String sendWater = str.substring(Index3+1, Index4);
        String sendMotion = str.substring(Index4+1, Index5);
        String sendBeing = str.substring(Index5+1, Index6);

        //Checks if there is motion
        int motion = sendMotion.toInt();
        if(motion == 1){
          Firebase.setBool("/plantStatus/greet", true);
        }else{
          Firebase.setBool("/plantStatus/greet", false);
        }

        //Checks if plant it's being watered
        int being = sendBeing.toInt();
        if(being == 1){
          Firebase.setBool("/plantStatus/beingWatered", true);
        }else{
          Firebase.setBool("/plantStatus/beingWatered", false);
        }

        //convert remaining data received to numerical data
        int newLight = sendLight.toInt();
        float newTemp = sendTemperature.toFloat();
        int newWater = sendWater.toInt();

        //'send' numerical data to firebase
        Firebase.setInt("/plantStatus/light",newLight);
        Firebase.setFloat("/plantStatus/temperature",newTemp);
        Firebase.setInt("/plantStatus/water",newWater);

        //when is receiving data from serial checks if automated watering is true or false and send to arduino
        if(Firebase.getBool("/plantStatus/autoWatering") == true){
          Serial.println(1);
        }else{
          Serial.println(Firebase.getBool("/plantStatus/autoWatering"));
        }
    }
    recconectWiFi();
}
/* WiFi Connection
 * Function responsible for connecting wifi module to the internet
 * 
 * Uses wifi credentials provided above
 */
void initWiFi() {
 delay(10);
 Serial.println();
 Serial.print("Connecting to: ");
 Serial.println(SSID);

 WiFi.begin(SSID, PASSWORD); //connect to network using SSID and PASSWORD
 while (WiFi.status() != WL_CONNECTED) { //while the status of the connection its different, keep  waiting until succed
   delay(100);
   Serial.print(".");
 }
 Serial.println("");
 Serial.print("Connected on ");
 Serial.print(SSID);
 Serial.println(" | IP: ");
 Serial.println(WiFi.localIP());
}

/* Reconect WiFi
 * 
 * Keep trying to recconect in case of a connection crash
 */
void recconectWiFi() {
 ///while the status of the connection its different, keep trying to connect
 while (WiFi.status() != WL_CONNECTED) {
   delay(100);
   Serial.print(".");
 }
}


