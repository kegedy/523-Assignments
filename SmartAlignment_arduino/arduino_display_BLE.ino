/**************************************************************************
 This is an example for our Monochrome OLEDs based on SSD1306 drivers

 Pick one up today in the adafruit shop!
 ------> http://www.adafruit.com/category/63_98

 This example is for a 128x32 pixel display using I2C to communicate
 3 pins are required to interface (two I2C and one reset).

 Adafruit invests time and resources providing this open
 source code, please support Adafruit and open-source
 hardware by purchasing products from Adafruit!

 Written by Limor Fried/Ladyada for Adafruit Industries,
 with contributions from the open source community.
 BSD license, check license.txt for more information
 All text above, and the splash screen below must be
 included in any redistribution.
 **************************************************************************
 * Water level example for the Nano 33 BLE (Sense)
 * You need version 2.0 or higher of the LMS9DS1 library to run this example 
 * 
 * Calibration makes the difference between a few degrees and within a degree accuracy
 * Run the DIY calibration program first and copy/paste the Accelerometer calibration data below where it's indicated.
 * 
 *   The circuit:
 *  - Arduino Nano 33 BLE (Sense)
 *  - or Arduino connected to LSM9DS1 breakout board
 * 
 * Written by Femme Verbeek 
 *     6 June 2020  
 * Released to the public domain
 **************************************************************************/


 

#include <SPI.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <Arduino_LSM9DS1.h>
#include <ArduinoBLE.h>

#define SCREEN_WIDTH 128 // OLED display width, in pixels
#define SCREEN_HEIGHT 32 // OLED display height, in pixels

void read_N_Accel(unsigned int N, float& averX, float& averY, float& averZ);
char *ftostrf (float val, signed char width, unsigned char prec, char *sout);

// BLE IMU Service
BLEService imuService("180F");

// BLE IMU Level Characteristic
BLEUnsignedCharCharacteristic imuLevelChar0("2A19",  // standard 16-bit characteristic UUID
    BLERead | BLENotify); // remote clients will be able to get notifications if this characteristic changes
//BLEUnsignedCharCharacteristic imuLevelChar1("2A20", BLERead | BLENotify);
//BLEUnsignedCharCharacteristic imuLevelChar2("2A21", BLERead | BLENotify);


long previousMillis = 0;  // counter for updating screen and BLE service

// Declaration for an SSD1306 display connected to I2C (SDA, SCL pins)
//   The pins for I2C are defined by the Wire-library. 
//     On an arduino UNO:       A4(SDA), A5(SCL)
//     On an arduino MEGA 2560: 20(SDA), 21(SCL)
//     On an arduino LEONARDO:   2(SDA),  3(SCL), ...
#define OLED_RESET     4 // Reset pin # (or -1 if sharing Arduino reset pin)
#define SCREEN_ADDRESS 0x3C ///< See datasheet for Address; 0x3D for 128x64, 0x3C for 128x32
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

#define LOGO_HEIGHT   16
#define LOGO_WIDTH    16
static const unsigned char PROGMEM logo_bmp[] =
{ 0b00000000, 0b11000000,
  0b00000001, 0b11000000,
  0b00000001, 0b11000000,
  0b00000011, 0b11100000,
  0b11110011, 0b11100000,
  0b11111110, 0b11111000,
  0b01111110, 0b11111111,
  0b00110011, 0b10011111,
  0b00011111, 0b11111100,
  0b00001101, 0b01110000,
  0b00011011, 0b10100000,
  0b00111111, 0b11100000,
  0b00111111, 0b11110000,
  0b01111100, 0b11110000,
  0b01110000, 0b01110000,
  0b00000000, 0b00110000 };

void drawPitch(float pitchx) {
    display.clearDisplay();
    display.setTextSize(2);
    display.setTextColor(SSD1306_WHITE);
    display.setCursor(32,8);
    display.print(String(pitchx,1));
    display.display();
}

void setup() {
  Serial.begin(9600);
  while (!Serial);

  // initialize the built-in LED pin to indicate when a central is connected
  pinMode(LED_BUILTIN, OUTPUT); 

  // SSD1306_SWITCHCAPVCC = generate display voltage from 3.3V internally
  if(!display.begin(SSD1306_SWITCHCAPVCC, SCREEN_ADDRESS)) {
    Serial.println(F("SSD1306 allocation failed"));
    for(;;); // Don't proceed, loop forever
  }
  // begin initialization
  if (!BLE.begin()) {
    Serial.println("starting BLE failed!");
    while (1);
  }
  if (!IMU.begin()) {
    Serial.println("Failed to initialize IMU!");
    while (1);
  }

    /* Set a local name for the BLE device
     This name will appear in advertising packets
     and can be used by remote devices to identify this BLE device
     The name can be changed but maybe be truncated based on space left in advertisement packet
  */
  BLE.setLocalName("SmartAlignment");
  BLE.setAdvertisedService(imuService); // add the service UUID
  imuService.addCharacteristic(imuLevelChar0); // add the imu level characteristic
//  imuService.addCharacteristic(imuLevelChar1);
//  imuService.addCharacteristic(imuLevelChar2);
  BLE.addService(imuService); // Add the imu service
  imuLevelChar0.writeValue(0); // set initial value for this characteristic

  /* Start advertising BLE.  It will start continuously transmitting BLE
     advertising packets and will be visible to remote BLE central devices
     until it receives a new connection */

  // start advertising
  BLE.advertise();
  Serial.println(imuService.uuid());
  Serial.println("Bluetooth device active, waiting for connections...");
}

float x, y, z, pitchx, pitchy;

void loop() {
  
  // wait for a BLE central
  BLEDevice central = BLE.central();

  // show sensor data while waiting
  long currentMillis = millis();
  // if 500ms have passed, update IMU:
    if (currentMillis - previousMillis >= 500) {
      previousMillis = currentMillis;
      read_N_Accel(50,x,y,z);
      if (abs(x)>0.1 || abs(z)>0.1) pitchx = atan2(x,z)*180/PI; 
      else pitchx=0;
      drawPitch(pitchx);
    }

  // if a central is connected to the peripheral:
  if (central) {
    Serial.print("Connected to central: ");
    // print the central's BT address:
    Serial.println(central.address());
    // turn on the LED to indicate the connection:
    digitalWrite(LED_BUILTIN, HIGH);

    // check the imu level every 500ms
    // while the central is connected:
    while (central.connected()) {
      currentMillis = millis();
      // if 500ms have passed, update IMU:
      if (currentMillis - previousMillis >= 500) {
        previousMillis = currentMillis;
        read_N_Accel(50,x,y,z);
        if (abs(x)>0.1 || abs(z)>0.1) pitchx = atan2(x,z)*180/PI; 
        else pitchx=0;
        imuLevelChar0.writeValue(int(pitchx));
        drawPitch(pitchx);
      }
    }
    // when the central disconnects, turn off the LED:
    digitalWrite(LED_BUILTIN, LOW);
    Serial.print("Disconnected from central: ");
    Serial.println(central.address());
  }
}

void read_N_Accel(unsigned int N, float& averX, float& averY, float& averZ) 
{    float x, y, z;
     averX=0; averY =0;averZ =0;
     for (int i=1;i<=N;i++)
     {  while (!IMU.accelAvailable());
        IMU.readAccel(x, y, z);
        averX += x;    averY += y;     averZ += z;
     } 
     averX /= float(N);    averY /= float(N);     averZ /= float(N);
}
