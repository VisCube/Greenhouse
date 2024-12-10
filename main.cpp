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
#define PIN_MOIST A1
#define PIN_TEMPERATURE A2
#define PIN_LAMP 2
#define PIN_SHOWER 3
#define PIN_COOLER 4
#define PIN_HEATER 5

/* Данные для передачи данных на сервер */
#define SERVER_ADDRESS "192.168.0.11"
#define SERVER_PORT 8000
#define SERVER_ENDPOINT_DEVICES "/api/devices/"
#define SERVER_ENDPOINT_SENSORS "/api/sensors/%d/"
#define SERVER_ENDPOINT_ACTUATORS "/api/actuators/%d/"
#define SERVER_ENDPOINT_MESSAGES "/api/topics/%d/?offset=%d"

/* Интервалы циклов */
#define INTERVAL_READINGS 1000L
#define INTERVAL_SERVER 60000L
#define INTERVAL_BLUETOOTH 0L
#define INTERVAL_DISPLAY 5000L

/* Заготовки для дисплея */
#define MESSAGE_READING "%d / %d %s"
#define MESSAGE_WIFI "WiFi: %s"
#define MESSAGE_BLE "BLE: %s"

/* Текущие показатели */
struct Readings {
    int light; // %
    int moist; // %
    int temperature; // °C
};

/* Эталонные значения */
struct References {
    int light;
    int moist;
    int temperature;
};

/* Данные для подключения к WiFi */
struct WiFiCon {
    const char *ssid; // Название сети
    const char *pass; // Пароль
};

/* Идентификаторы компонентов на сервере */
struct IDs {
    int device; // Теплица
    int light, moist, temperature; // Сенсоры
    int lamp, shower, cooler, heater; // Актуаторы
};

/* Объекты для работы с датчиком температуры */
OneWire oneWire(PIN_TEMPERATURE);
DallasTemperature temperatureSensor(&oneWire);

/* Объекты для работы с дисплеем */
LiquidCrystal_I2C lcd(0x27, 32, 2);
int displayState = 0;

/* Объекты для работы с WiFi модулем */
WiFiClient wifiClient;
HttpClient httpClient(wifiClient, SERVER_ADDRESS, SERVER_PORT);

/* Объекты для работы с Bluetooth модулем */
BLEService ledService("180A");
BLEStringCharacteristic switchCharacteristic("2A57", BLERead | BLEWrite, 100);
BLEDevice central;

/* Объекты в памяти */
Readings readings;
References references;
WiFiCon wifiCon;
IDs ids;
int messagesCounter;
FlashStorage(referencesStorage, References);
FlashStorage(wifiStorage, WiFiCon);
FlashStorage(idsStorage, IDs);
FlashStorage(messagesStorage, int);

/* Счётчики циклов */
unsigned long lastReadings;
unsigned long lastServer;
unsigned long lastBluetooth;
unsigned long lastDisplay;

/* Считывает показания датчиков */
void readSensors() {
    readings.light = !digitalRead(PIN_LIGHT) * 37 + 42;
    readings.moist = map(1023 - analogRead(PIN_MOIST), 0, 1023, 0, 100);
    temperatureSensor.requestTemperatures();
    readings.temperature = static_cast<int>(temperatureSensor.getTempCByIndex(0));
}

/* Сверяет показания датчиков с эталонными */
void checkActuators() {
    /* Добавочные значения - погрешность, чтоб не удерживать точное значение */
    digitalWrite(PIN_LAMP, readings.light < references.light - 10);
    digitalWrite(PIN_SHOWER, readings.moist < references.moist - 10);
    digitalWrite(PIN_COOLER, readings.temperature > references.temperature + 5);
    digitalWrite(PIN_HEATER, readings.temperature < references.temperature - 5);
}

/* Проверяет адекватность эталонных показаний */
void checkReferences() {
    const bool shouldSave = references.light < 0 || references.light > 80 ||
                            references.moist < 0 || references.moist > 80 ||
                            references.temperature < 10 || references.temperature > 40;
    if (references.light < 0) references.light = 0; // Отрицательное освещение
    if (references.light > 80) references.light = 80; // Слишком яркая лампа
    if (references.moist < 0) references.moist = 0; // Отрицательная влажность
    if (references.moist > 80) references.moist = 80; // Слишком влажная почва
    if (references.temperature < 10) references.temperature = 10; // Слишком холодно
    if (references.temperature > 40) references.temperature = 40; // Слишком жарко
    if (shouldSave) referencesStorage.write(references);
}

/* Выводит строку на дисплей */
void displayInfo(const String &line) {
    Serial.println(line);
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print(line);
}

/* Выводит две строки на дисплей */
void displayInfo(char line1[], char line2[]) {
    Serial.println(line1);
    Serial.println(line2);
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print(line1);
    lcd.setCursor(0, 1);
    lcd.print(line2);
}

/* Выводит всю информацию на дисплей */
void displayAll() {
    char displayBuffer1[50];
    char displayBuffer2[50];
    switch (displayState) {
        case 0:
            sprintf(displayBuffer1, "Light:");
            sprintf(displayBuffer2, MESSAGE_READING, readings.light, references.light, "%");
            break;
        case 1:
            sprintf(displayBuffer1, "Moisture:");
            sprintf(displayBuffer2, MESSAGE_READING, readings.moist, references.moist, "%");
            break;
        case 2:
            sprintf(displayBuffer1, "Temperature:");
            sprintf(displayBuffer2, MESSAGE_READING, readings.temperature, references.temperature, "C");
            break;
        case 3:
            sprintf(displayBuffer1, MESSAGE_WIFI, WiFi.status() == WL_CONNECTED ? "+" : "-");
            sprintf(displayBuffer2, MESSAGE_BLE, BLE.connected() ? "+" : "-");
            break;
        default: ;
    }
    displayInfo(displayBuffer1, displayBuffer2);
    displayState = (displayState + 1) % 4;
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
    requestJSON["device"] = ids.device;
    requestJSON["type"] = type;
    JSONVar responseJSON = post(endpoint, requestJSON);
    return responseJSON["id"];
}

/* Регистрирует устройство и все компоненты на сервере и сохраняет их в памяти */
void checkRegistry() {
    const bool shouldSave = !(ids.device && ids.light && ids.moist && ids.temperature &&
                              ids.lamp && ids.shower && ids.cooler && ids.heater);
    if (!ids.device) ids.device = registerDevice();
    if (!ids.light) ids.light = registerComponent(SERVER_ENDPOINT_SENSORS, "light");
    if (!ids.moist) ids.moist = registerComponent(SERVER_ENDPOINT_SENSORS, "moist");
    if (!ids.temperature) ids.temperature = registerComponent(SERVER_ENDPOINT_SENSORS, "temperature");
    if (!ids.lamp) ids.lamp = registerComponent(SERVER_ENDPOINT_ACTUATORS, "lamp");
    if (!ids.shower) ids.shower = registerComponent(SERVER_ENDPOINT_ACTUATORS, "shower");
    if (!ids.cooler) ids.cooler = registerComponent(SERVER_ENDPOINT_ACTUATORS, "cooler");
    if (!ids.heater) ids.heater = registerComponent(SERVER_ENDPOINT_ACTUATORS, "heater");
    if (shouldSave) idsStorage.write(ids);
}

/* Отправляет сведения о сенсоре на сервер */
void patchSensor(const int id, const int reading, const int reference) {
    char endpointBuffer[100];
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
    patchSensor(ids.light, readings.light, references.light);
    patchSensor(ids.moist, readings.moist, references.moist);
    patchSensor(ids.temperature, readings.temperature, references.temperature);
    patchActuator(ids.lamp, readings.light < references.light - 10);
    patchActuator(ids.shower, readings.moist < references.moist - 10);
    patchActuator(ids.cooler, readings.temperature > references.temperature + 5);
    patchActuator(ids.heater, readings.temperature < references.temperature - 5);
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

/* Парсит команду и выполняет её */
void parseMessage(JSONVar message) {
    const String command = message["command"];
    if (command == "setReference") {
        const String sensor = message["sensor"];
        const int value = message["value"];
        if (sensor == "light") references.light = value;
        if (sensor == "moist") references.moist = value;
        if (sensor == "temperature") references.temperature = value;
        referencesStorage.write(references);
    }
    if (command == "setWiFi") {
        wifiCon.ssid = message["ssid"];
        wifiCon.pass = message["pass"];
        wifiStorage.write(wifiCon);
    }
}

/* Получает новые команды с сервера */
void pollServer() {
    char endpointBuffer[100];
    sprintf(endpointBuffer, SERVER_ENDPOINT_MESSAGES, ids.device, messagesCounter);

    httpClient.get(endpointBuffer);
    const String responseBody = httpClient.responseBody();
    JSONVar responseJSON = JSON.parse(responseBody);

    const int newMessages = responseJSON["count"];
    for (int i = 0; i < newMessages; i++)
        parseMessage(responseJSON["messages"][i]["payload"]);
    messagesCounter += newMessages;
    if (newMessages) messagesStorage.write(messagesCounter);
}

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

void setup() {
    Serial.begin(9600);
    while (!Serial) {
    }
    lcd.init();
    lcd.backlight();
    displayInfo("Initializing...");

    pinMode(PIN_LIGHT, INPUT);
    pinMode(PIN_MOIST, INPUT);
    pinMode(PIN_TEMPERATURE, INPUT);
    temperatureSensor.begin();
    pinMode(PIN_LAMP, OUTPUT);
    pinMode(PIN_SHOWER, OUTPUT);
    pinMode(PIN_COOLER, OUTPUT);
    pinMode(PIN_HEATER, OUTPUT);

    references = referencesStorage.read();
    wifiCon = wifiStorage.read();
    ids = idsStorage.read();
    messagesCounter = messagesStorage.read();

    initBluetooth();
}

int iterCounter = 0;
void loop() {
    readSensors();
    checkReferences();
    checkActuators();

    if (checkWiFi() && iterCounter++ == 10) {
        displayInfo("Sending data...");
        checkRegistry();
        patchAll();
        pollServer();
        displayInfo("Data received.");
        iterCounter = 0;
    }
    pollBluetooth();

    displayAll();
    delay(1000);
}
