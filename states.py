from aiogram.fsm.state import State, StatesGroup

from config import config
from server import get_greenhouse
from server.models import Greenhouse, Sensor

DEVICE_ID = config.device_id.get_secret_value()  # id демонстрационной теплицы


class MenuStates(StatesGroup):
    IN_MAIN_MENU = State()
    CHOOSING_SENSOR = State()
    PENDING_REFERENCE = State()


# Чтобы не обращаться к серверу каждый раз, когда нужны данные о теплице
async def put_to_cache(user_id: int, greenhouse: Greenhouse | None):
    cached_greenhouses.update({user_id: greenhouse})


async def get_from_cache(user_id: int) -> Greenhouse:
    greenhouse = cached_greenhouses.get(user_id)
    if greenhouse is not None:
        return greenhouse

    greenhouse = get_greenhouse(DEVICE_ID)
    if greenhouse is not None:
        await put_to_cache(user_id, greenhouse)
        return greenhouse


async def remove_from_cache(user_id: int):
    chosen_sensors.update({user_id: None})


# Чтобы запомнить, эталон какого датчика меняет юзер
async def choose_sensor(user_id: int, sensor: Sensor):
    chosen_sensors.update({user_id: sensor})


async def get_chosen_sensor(user_id: int) -> Sensor:
    return chosen_sensors.get(user_id, Sensor(0, "", 0, 0))


cached_greenhouses = {}
chosen_sensors = {}
