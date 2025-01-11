#include "Arduino.h"
#include <ArduinoBLE.h>
#include <ArduinoHttpClient.h>
#include <Arduino_JSON.h>
#include <DallasTemperature.h>
#include <FlashStorage.h>
#include <LiquidCrystal_I2C.h>
#include <OneWire.h>
#include <WiFiNINA.h>

/* Входы/выходы компонентов */
#define PIN_LIGHT A0
#define PIN_TEMPERATURE A1
#define PIN_MOISTURE A2
#define PIN_LAMP 2
#define PIN_COOLER 3
#define PIN_HEATER 4
#define PIN_SHOWER 5

/* Шаблоны дисплея */
#define MESSAGE_READING "%d / %d %s"
#define MESSAGE_WIFI "WiFi: %s"
#define MESSAGE_BLE "BLE: %s"

/* Данные для связи с сервером */
#define SERVER_ADDRESS "metallolom.pythonanywhere.com"
#define SERVER_PORT 80
#define SERVER_ENDPOINT_DEVICES "/api/devices/"
#define SERVER_ENDPOINT_SENSORS "/api/sensors/%d/"
#define SERVER_ENDPOINT_ACTUATORS "/api/actuators/%d/"
#define SERVER_ENDPOINT_MESSAGES "/api/topics/%d/?offset=%d"

/* Интервалы циклов */
#define INTERVAL_READINGS 1000L
#define INTERVAL_SERVER 30000L
#define INTERVAL_BLUETOOTH 0L
#define INTERVAL_DISPLAY 1000L


/* Сенсоры */
struct Sensor {
    int pin; // Пин для чтения
    int reading{}; // Текущее значение
    int reference{}; // Эталонное значение
    int id{}; // Идентификатор на сервере

    explicit Sensor(const int pin): pin(pin) {
    }
};

/* Актуаторы */
struct Actuator {
    int pin; // Управляющий пин
    bool status{}; // Текущее состояние (вкл/выкл)
    int id{}; // Идентификатор на сервере

    explicit Actuator(const int pin) : pin(pin) {
    }
};

/* Данные для подключения к WiFi */
struct WiFiCon {
    const char *ssid; // Название сети
    const char *pass; // Пароль
};


// Датчик света, датчик температуры, датчик влажности почвы
Sensor light(PIN_LIGHT), temperature(PIN_TEMPERATURE), moisture(PIN_MOISTURE);
// Лампа, кулер, нагреватель, помпа для полива
Actuator lamp(PIN_LAMP), cooler(PIN_COOLER), heater(PIN_HEATER), shower(PIN_SHOWER);

/* Объекты для работы с датчиком температуры */
OneWire oneWire(PIN_TEMPERATURE);
DallasTemperature temperatureSensor(&oneWire);

/* Объекты для работы с дисплеем */
LiquidCrystal_I2C lcd(0x27, 32, 2);
int displayState;

/* Объекты для работы с Bluetooth модулем */
BLEService ledService("180A");
BLEStringCharacteristic switchCharacteristic("2A57", BLERead | BLEWrite, 100);
BLEDevice central;

/* Объекты для работы с WiFi модулем */
WiFiClient wifiClient;
HttpClient httpClient(wifiClient, SERVER_ADDRESS, SERVER_PORT);
WiFiCon wifiCon;
int deviceID;
int messageCounter;

/* Объекты для работы с памятью */
FlashStorage(lightStorage, Sensor);
FlashStorage(temperatureStorage, Sensor);
FlashStorage(moistureStorage, Sensor);
FlashStorage(lampStorage, Actuator);
FlashStorage(coolerStorage, Actuator);
FlashStorage(heaterStorage, Actuator);
FlashStorage(showerStorage, Actuator);
FlashStorage(wifiStorage, WiFiCon);
FlashStorage(deviceStorage, int);
FlashStorage(messageStorage, int);

/* Счётчики циклов */
unsigned long lastReadings;
unsigned long lastServer;
unsigned long lastBluetooth;
unsigned long lastDisplay;


/* Считывает показания датчиков */
void readSensors() {
    temperatureSensor.requestTemperatures();

    light.reading = !digitalRead(light.pin) * 37 + 42;
    temperature.reading = static_cast<int>(temperatureSensor.getTempCByIndex(0));
    moisture.reading = map(1023 - analogRead(moisture.pin), 0, 1023, 0, 100);
}

/* Проверяет адекватность эталонных показаний */
void checkReferences() {
    if (light.reference < 0) {
        // Слишком темно
        light.reference = 0;
        lightStorage.write(light);
    } else if (light.reference > 1000) {
        // Слишком светло
        light.reference = 1000;
        lightStorage.write(light);
    }

    if (temperature.reference < 10) {
        // Слишком холодно
        temperature.reference = 10;
        temperatureStorage.write(temperature);
    } else if (temperature.reference > 40) {
        // Слишком жарко
        temperature.reference = 40;
        temperatureStorage.write(temperature);
    }

    if (moisture.reference < 0) {
        // Слишком сухо
        moisture.reference = 0;
        moistureStorage.write(moisture);
    } else if (moisture.reference > 70) {
        // Слишком влажно
        moisture.reference = 70;
        moistureStorage.write(moisture);
    }
}

/* Сверяет показания датчиков с эталонными */
void useActuators() {
    /* Добавочные значения - погрешность, чтоб не удерживать точное значение */
    lamp.status = light.reading < light.reference - 100;
    cooler.status = temperature.reading < temperature.reference - 5;
    heater.status = temperature.reading > temperature.reference + 5;
    shower.status = moisture.reading < moisture.reference - 10;

    digitalWrite(lamp.pin, lamp.status);
    digitalWrite(cooler.pin, cooler.status);
    digitalWrite(heater.pin, heater.status);
    digitalWrite(shower.pin, shower.status);
}

/* Выводит две строки на дисплей */
void displayData(const char *line1, const char *line2) {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print(line1);
    lcd.setCursor(0, 1);
    lcd.print(line2);
}

/* Итеративно выводит всю информацию на дисплей */
void displayAll() {
    char line1[16], line2[16];
    switch (displayState) {
        case 0:
            sprintf(line1, "Light:");
            sprintf(line2, "%d / %d %s", light.reading, light.reference, "Lx");
            break;
        case 1:
            sprintf(line1, "Temperature:");
            sprintf(line2, "%d / %d %s", temperature.reading, temperature.reference, "C");
            break;
        case 2:
            sprintf(line1, "Moisture:");
            sprintf(line2, "%d / %d %s", moisture.reading, moisture.reference, "%");
            break;
        case 3:
            sprintf(line1, MESSAGE_WIFI, WiFi.status() == WL_CONNECTED ? "+" : "-");
            sprintf(line2, MESSAGE_BLE, BLE.connected() ? "+" : "-");
            break;
        default: ;
    }
    displayState = (displayState + 1) % 4;
    displayData(line1, line2);
}

/* Парсит команду и выполняет её */
void parseMessage(JSONVar message) {
    const String command = message["command"];
    if (command == "setReference") {
        const String sensor = message["sensor"];
        const int value = message["value"];
        switch (sensor) {
            case "light": {
                light.reference = value;
                lightStorage.write(light);
                break;
            }
            case "temperature": {
                temperature.reference = value;
                temperatureStorage.write(temperature);
                break;
            }
            case "moisture": {
                moisture.reference = value;
                moistureStorage.write(moisture);
                break;
            }
            default: ;
        }
    }
    if (command == "setWiFi") {
        wifiCon.ssid = message["ssid"];
        wifiCon.pass = message["pass"];
        wifiStorage.write(wifiCon);
    }
    if (command == "getData") {
        central = BLE.central();
        if (central) {
            while (central.connected()) {
                JSONVar fullData;
                JSONVar sensorData;
                JSONVar actuatorData;

                JSONVar lightData;
                lightData["id"] = light.id;
                lightData["type"] = "light";
                lightData["reading"] = light.reading;
                lightData["reference"] = light.reference;
                sensorData[0] = lightData;
                JSONVar temperatureData;
                temperatureData["id"] = temperature.id;
                temperatureData["type"] = "temperature";
                temperatureData["reading"] = temperature.reading;
                temperatureData["reference"] = temperature.reference;
                sensorData[1] = temperatureData;
                JSONVar moistureData;
                moistureData["id"] = moisture.id;
                moistureData["type"] = "moisture";
                moistureData["reading"] = moisture.reading;
                moistureData["reference"] = moisture.reference;
                sensorData[2] = moistureData;

                JSONVar lampData;
                lampData["id"] = lamp.id;
                lampData["type"] = "lamp";
                lampData["status"] = lamp.status;
                actuatorData[0] = lampData;
                JSONVar coolerData;
                coolerData["id"] = cooler.id;
                coolerData["type"] = "cooler";
                coolerData["status"] = cooler.status;
                actuatorData[0] = coolerData;
                JSONVar heaterData;
                heaterData["id"] = heater.id;
                heaterData["type"] = "heater";
                heaterData["status"] = heater.status;
                actuatorData[0] = heaterData;
                JSONVar showerData;
                showerData["id"] = shower.id;
                showerData["type"] = "shower";
                showerData["status"] = shower.status;
                actuatorData[0] = showerData;

                fullData["id"] = deviceID;
                fullData["sensors"] = sensorData;
                fullData["actuators"] = actuatorData;
                switchCharacteristic.writeValue(JSON.stringify(fullData));
            }
        }
    }
}

/* Включает Bluetooth */
void initBluetooth() {
    if (!BLE.begin()) return;
    BLE.setLocalName("Nano 33 IoT");
    BLE.setAdvertisedService(ledService);
    ledService.addCharacteristic(switchCharacteristic);
    BLE.addService(ledService);
    BLE.advertise();
}

/* Получает данные по Bluetooth */
void pollBluetooth() {
    central = BLE.central();
    if (central) {
        while (central.connected()) {
            if (switchCharacteristic.written()) {
                parseMessage(JSON.parse(switchCharacteristic.value()));
            }
        }
    }
}

/* Поддерживает подключение к WiFi */
bool checkWiFi() {
    if (WiFi.status() == WL_CONNECTED) return true;
    return WiFi.begin(wifiCon.ssid, wifiCon.pass) == WL_CONNECTED;
}

/* Отправляет POST-запрос на сервер и возвращает ответ в формате JSON */
JSONVar post(const char *endpoint, const JSONVar &requestJSON) {
    const String requestBody = JSON.stringify(requestJSON);
    httpClient.post(endpoint, "application/json", requestBody);
    const String responseBody = httpClient.responseBody();
    JSONVar responseJSON = JSON.parse(responseBody);
    return responseJSON;
}

/* Регистрирует устройство на сервере и возвращает его идентификатор */
int registerDevice() {
    const JSONVar requestJSON;
    JSONVar responseJSON = post(SERVER_ENDPOINT_DEVICES, requestJSON);
    return responseJSON["id"];
}

/* Регистрирует сенсор/актуатор на сервере и возвращает его идентификатор */
int registerComponent(const char *endpoint, const char *type) {
    JSONVar requestJSON;
    requestJSON["device"] = deviceID;
    requestJSON["type"] = type;
    JSONVar responseJSON = post(endpoint, requestJSON);
    return responseJSON["id"];
}


/* Регистрирует устройство и все компоненты на сервере и сохраняет их в памяти */
void checkRegistry() {
    if (!deviceID) {
        deviceID = registerDevice();
        deviceStorage.write(deviceID);
    }

    if (!light.id) {
        light.id = registerComponent(SERVER_ENDPOINT_SENSORS, "light");
        lightStorage.write(light);
    }
    if (!temperature.id) {
        temperature.id = registerComponent(SERVER_ENDPOINT_SENSORS, "temperature");
        temperatureStorage.write(temperature);
    }
    if (!moisture.id) {
        moisture.id = registerComponent(SERVER_ENDPOINT_SENSORS, "moisture");
        moistureStorage.write(moisture);
    }

    if (!lamp.id) {
        lamp.id = registerComponent(SERVER_ENDPOINT_ACTUATORS, "lamp");
        lampStorage.write(lamp);
    }
    if (!cooler.id) {
        cooler.id = registerComponent(SERVER_ENDPOINT_ACTUATORS, "cooler");
        coolerStorage.write(cooler);
    }
    if (!heater.id) {
        heater.id = registerComponent(SERVER_ENDPOINT_ACTUATORS, "heater");
        heaterStorage.write(heater);
    }
    if (!shower.id) {
        shower.id = registerComponent(SERVER_ENDPOINT_ACTUATORS, "shower");
        showerStorage.write(shower);
    }
}

/* Отправляет сведения о сенсоре на сервер */
void patchSensor(const int id, const int reading, const int reference) {
    char endpointBuffer[64];
    sprintf(endpointBuffer, SERVER_ENDPOINT_SENSORS, id);

    JSONVar requestJSON;
    requestJSON["reading"] = reading;
    requestJSON["reference"] = reference;
    const String requestBody = JSON.stringify(requestJSON);

    httpClient.patch(endpointBuffer, "application/json", requestBody);
    httpClient.responseBody();
}

/* Отправляет сведения об актуаторе на сервер */
void patchActuator(const int id, const bool status) {
    char endpointBuffer[100];
    sprintf(endpointBuffer, SERVER_ENDPOINT_ACTUATORS, id);

    JSONVar requestJSON;
    requestJSON["status"] = status;
    const String requestBody = JSON.stringify(requestJSON);

    httpClient.patch(endpointBuffer, "application/json", requestBody);
    httpClient.responseBody();
}

/* Отправляет сведения о всех компонентах на сервер */
void patchAll() {
    patchSensor(light.id, light.reading, light.reference);
    patchSensor(moisture.id, moisture.reading, moisture.reference);
    patchSensor(temperature.id, temperature.reading, temperature.reference);
    patchActuator(lamp.id, lamp.status);
    patchActuator(cooler.id, cooler.status);
    patchActuator(heater.id, heater.status);
    patchActuator(shower.id, shower.status);
}

/* Получает новые команды с сервера */
void pollServer() {
    char endpointBuffer[64];
    sprintf(endpointBuffer, SERVER_ENDPOINT_MESSAGES, deviceID, messageCounter);

    httpClient.get(endpointBuffer);
    const String responseBody = httpClient.responseBody();
    JSONVar responseJSON = JSON.parse(responseBody);

    const int newMessages = responseJSON["count"];
    for (int i = 0; i < newMessages; i++)
        parseMessage(responseJSON["messages"][i]["payload"]);
    messageCounter += newMessages;
    if (newMessages) messageStorage.write(messageCounter);
}

/* Процедура обмена данных с сервером */
void iterServer() {
    if (checkWiFi()) {
        displayData("Sending data...", "");
        checkRegistry();
        patchAll();
        displayData("", "Data sent.");
        displayData("Receiving data...", "");
        pollServer();
        displayData("", "Data received.");
    }
}

void setup() {
    Serial.begin(9600);
    while (!Serial) {
    }

    pinMode(PIN_LIGHT, INPUT);
    pinMode(PIN_TEMPERATURE, INPUT);
    pinMode(PIN_MOISTURE, INPUT);
    temperatureSensor.begin();

    pinMode(PIN_LAMP, OUTPUT);
    pinMode(PIN_SHOWER, OUTPUT);
    pinMode(PIN_COOLER, OUTPUT);
    pinMode(PIN_HEATER, OUTPUT);

    lcd.init();
    lcd.backlight();
    displayData("Initializing...", "");

    light = lightStorage.read();
    temperature = temperatureStorage.read();
    moisture = moistureStorage.read();
    lamp = lampStorage.read();
    cooler = coolerStorage.read();
    heater = heaterStorage.read();
    shower = showerStorage.read();
    wifiCon = wifiStorage.read();
    messageCounter = messageStorage.read();

    initBluetooth();
}

void loop() {
    if (millis() - lastReadings > INTERVAL_READINGS) {
        readSensors();
        checkReferences();
        useActuators();
        lastReadings = millis();
    }
    if (millis() - lastBluetooth > INTERVAL_BLUETOOTH) {
        pollBluetooth();
        lastBluetooth = millis();
    }
    if (millis() - lastServer > INTERVAL_SERVER) {
        iterServer();
        lastServer = millis();
    }
    if (millis() - lastDisplay > INTERVAL_DISPLAY) {
        displayAll();
        lastDisplay = millis();
    }
}
