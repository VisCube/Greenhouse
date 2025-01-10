import math

from aiogram.types import (
    KeyboardButton,
    ReplyKeyboardMarkup,
    ReplyKeyboardRemove
)

from server import Greenhouse
from templates import *


def get_empty_keyboard():
    return ReplyKeyboardRemove(remove_keyboard=True)


def get_back_keyboard():
    buttons = [
        [KeyboardButton(text=BUTTON_BACK)]
    ]
    return ReplyKeyboardMarkup(keyboard=buttons, resize_keyboard=True)


def get_menu_keyboard():
    buttons = [
        [KeyboardButton(text=BUTTON_REFERENCE),
         KeyboardButton(text=BUTTON_REFRESH)]
    ]
    return ReplyKeyboardMarkup(keyboard=buttons, resize_keyboard=True)


async def get_sensor_keyboard(greenhouse: Greenhouse):
    size = math.ceil(math.sqrt(len(greenhouse.sensors)))
    buttons = []
    for row in range(size):
        buttons.append([])
        for col in range(size):
            if (pos := row * size + col) >= len(greenhouse.sensors):
                break
            sensor = greenhouse.sensors[pos]
            buttons[row].append(
                KeyboardButton(
                    text=BUTTON_REFERENCE_SENSOR.format(
                        EMOJIS_SENSORS.get(sensor.type), sensor.id
                    )
                )
            )
    buttons.append([KeyboardButton(text=BUTTON_BACK)])
    return ReplyKeyboardMarkup(keyboard=buttons, resize_keyboard=True)
