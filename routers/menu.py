from aiogram import Router, F
from aiogram.filters import Command
from aiogram.fsm.context import FSMContext
from aiogram.types import Message

from config import config
from keyboards import get_menu_keyboard
from routers.reference import command_reference
from server import get_greenhouse
from states import MenuStates, get_from_cache, put_to_cache
from templates import *

DEVICE_ID = config.device_id.get_secret_value()  # id демонстрационной теплицы
router = Router()


@router.message(Command(COMMAND_MENU))
async def command_menu(message: Message, state: FSMContext):
    greenhouse = await get_from_cache(message.from_user.id)
    if not greenhouse:
        return MESSAGE_MENU_404

    sensor_lines = [
        MESSAGE_SENSOR.format(
            sensor.id,
            EMOJIS_SENSORS.get(sensor.type),
            sensor.reading,
            sensor.reference
        )
        for sensor in greenhouse.sensors
    ]

    actuator_lines = [
        MESSAGE_ACTUATOR.format(
            actuator.id,
            EMOJIS_SENSORS.get(actuator.type),
            EMOJIS_STATUSES.get(actuator.status)
        )
        for actuator in greenhouse.actuators
    ]

    await state.set_state(MenuStates.IN_MAIN_MENU)
    await message.answer(
        MESSAGE_MENU.format(
            # greenhouse.id, демо-теплица...
            "\n".join(sensor_lines),
            "\n".join(actuator_lines)
        ),
        reply_markup=get_menu_keyboard()
    )


@router.message(F.text == COMMAND_REFRESH)
@router.message(F.text == BUTTON_REFRESH)
async def command_refresh(message: Message, state: FSMContext):
    greenhouse = get_greenhouse(DEVICE_ID)
    if greenhouse is not None:
        await put_to_cache(message.from_user.id, greenhouse)
    return await command_menu(message, state)


@router.message(F.text == BUTTON_BACK, MenuStates.PENDING_REFERENCE)
async def back_to_sensor(message: Message, state: FSMContext):
    await command_reference(message, state)


@router.message(F.text == BUTTON_BACK)
async def back_to_menu(message: Message, state: FSMContext):
    await command_menu(message, state)
