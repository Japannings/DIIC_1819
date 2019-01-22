/* DIIC Project - Talking Plant
 * 
 * Author: Group A2 - IST
 * Last Edited: 20/12/2018
 * Version: 7
 * 
 * This project, made for Arduino, controls
 * a few sensors on a plant watering device.
 * The values from the sensores are converted
 * in a human understanble values and then they 
 * are sent to a Wi-Fi module.
 * 
 * The architecture chosen for this device is
 * a simple Round-Robin, measuring each sensor
 * on every cycle.
*/

//Sensors and Actuators
#define tempSensorPin A1
#define lightSensorPin A0
#define moistureSensorPin A2
#define trigPin 12
#define echoPin 13
#define motorPin 9

//Values
float temperatureValue;
float temperature = 99.9;
String light;
String moisture;
String sendData = "";
String str = "";
int waterValue;
//movement sensor
int previousDistance = 0;

//Watering variables
bool autoWater = false;
bool watering = false;
long previousWater = 0;

void setup() {
  Serial.begin(115200);

  pinMode(lightSensorPin, INPUT);
  pinMode(tempSensorPin, INPUT);
  pinMode(moistureSensorPin, INPUT);
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);
  pinMode(motorPin, OUTPUT);
}

void loop() {
  unsigned long currentTime = millis();

  if (Serial.available() > 0) {
    str = Serial.readString();
    if (str.toInt() == 1) {
      autoWater = true;
    } else {
      autoWater = false;
    }
  }

  updateTemp();
  updateLight();
  updateMoisture();
  detectMovement();
  waterPlant();

  Serial.println(sendData);
  sendData = "";
  while(millis() < currentTime + 2000){
  }
}

/* Water Pump
 *  
 * If the automatic watering flag is TRUE and the
 * soil moisture is either "Dry" or "Very Dry",
 * it turns the pump on for 2 seconds, only turning
 * it again after 30 seconds to let the water sink in
 * first (and if the above conditions are met again).
*/
void waterPlant() {

  if (autoWater && (waterValue < 3) && (millis() - previousWater > 30000 || previousWater == 0)) {
    digitalWrite(motorPin, HIGH);
    delay(2000);
    digitalWrite(motorPin, LOW);
    previousWater = millis();
    watering = true;
    sendData = sendData + "|" + 1;
  } else {
    watering = false;
    sendData = sendData + "|" + 0;
  }
}
/* Ultrasound Sensor
 *  
 *  By comparing distances, detects movement in front of the plant.
 *  If movement is detected, it sends a "1", if not, "0".
*/
void detectMovement() {
  // Clears the trigPin
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);

  // Sets the trigPin on HIGH state for 10 micro seconds
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

  // Reads the echoPin, returns the sound wave travel time in microseconds
  long duration = pulseIn(echoPin, HIGH);

  // Calculating the distance
  int distance = duration * 0.034 / 2;

  if (distance < 200 && previousDistance - distance > 10) {
    sendData = sendData + "|" + 1;
  } else {
    sendData = sendData + "|" + 0;
  }

  previousDistance = distance;
}
/* Temperature Sensor
 *  
 * Measures the environment's temperature and sends
 * it as a float value.
*/
void updateTemp() {
  int tempSensorValue = analogRead(tempSensorPin);
  delay(1);
  float voltage = (tempSensorValue / 1024.0) * 5.0; //turns the analog signal to Celsius temperature
  temperatureValue = (voltage - .5) * 100.0;

  if(temperatureValue <= 40.00 && temperatureValue >= 0.00 && ((temperature < 99.9 && abs(temperature-temperatureValue) < 10) || temperature == 99.9)){ //filters sensor errors
     temperature = temperatureValue;
  }  
  sendData = sendData + "|" + temperature; 
}

/* Light Sensor
 *  
 * Turns the signal measured by the sensor from 1 to 5 linearly,  
 * 1 being the "Very Low" and 5 being "Very High".
*/
void updateLight() {
  int lightSensorValue = analogRead(lightSensorPin);
  delay(1);

  int value = map(lightSensorValue, 0, 1000, 1, 5);
  light = value;
  sendData = sendData + "|" + value;
}

/* Moisture Sensor
 *  
 * Turns the signal measured by the sensor from 1 to 5 linearly, 
 * 1 being the "Very Dry" and 5 being "Soaked".
 * Since the sensor might not be connected at all times,
 * any value above 900 is considered "Unknown" instead of "Very Dry"
*/
void updateMoisture() {
  int moistureSensorValue = analogRead(moistureSensorPin);
  delay(1);
  if (moistureSensorValue < 900) {
    waterValue = map(moistureSensorValue, 0, 1000, 5, 1);
  } else {
    waterValue = 6;
  }
  moisture = waterValue;
  sendData = sendData + "|" + waterValue;
}
