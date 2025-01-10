import requests

from config import config
from .models import Greenhouse, Sensor, Actuator

BASE_URL = config.server_url.get_secret_value()


def get_greenhouse(device_id) -> Greenhouse | None:
    response = requests.get(f"{BASE_URL}/api/devices/{device_id}/")
    if response.status_code != 200:
        return None

    data = response.json()
    sensors = [
        Sensor(
            sensor_data["id"],
            sensor_data["type"],
            sensor_data["reading"],
            sensor_data["reference"]
        )
        for sensor_data in data["sensors"]
    ]
    actuators = [
        Actuator(
            controller_data["id"],
            controller_data["type"],
            controller_data["status"]
        )
        for controller_data in data["actuators"]
    ]

    return Greenhouse(data["id"], sensors, actuators)


def update_reference(device_id: int, sensor: Sensor, reference: int) -> bool:
    message = {
        "command": "setReference",
        "sensor": sensor.type,
        "value": reference
    }
    response = requests.post(
        f"{BASE_URL}/api/topics/{device_id}/",
        json=dict(payload=message)
    )
    return response.status_code == 201
