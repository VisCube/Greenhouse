from aiogram import Router, F
from aiogram.fsm.context import FSMContext
from aiogram.types import Message

from keyboards import get_sensor_keyboard, get_back_keyboard
from server import update_reference
from states import MenuStates, get_from_cache, choose_sensor, get_chosen_sensor
from templates import *

router = Router()


@router.message(F.text == BUTTON_REFERENCE)
async def command_reference(message: Message, state: FSMContext):
    greenhouse = await get_from_cache(message.from_user.id)
    await state.set_state(MenuStates.CHOOSING_SENSOR)
    await message.answer(
        MESSAGE_REFERENCE_CHOOSING,
        reply_markup=await get_sensor_keyboard(greenhouse)
    )


@router.message(MenuStates.CHOOSING_SENSOR)
async def button_reference(message: Message, state: FSMContext):
    sensor_id_str = message.text.split()[-1]
    if not sensor_id_str.isnumeric():
        await message.reply(MESSAGE_INPUT_INVALID)

    sensor_id = int(sensor_id_str)
    greenhouse = await get_from_cache(message.from_user.id)

    for sensor in greenhouse.sensors:
        if sensor.id == sensor_id:
            await choose_sensor(message.from_user.id, sensor)
            await state.set_state(MenuStates.PENDING_REFERENCE)
            return await message.reply(
                MESSAGE_REFERENCE_PENDING,
                reply_markup=get_back_keyboard()
            )

    await message.reply(MESSAGE_REFERENCE_INVALID_SENSOR)


@router.message(MenuStates.PENDING_REFERENCE)
async def input_reference(message: Message, state: FSMContext):
    if not message.text.isnumeric():
        return await message.reply(MESSAGE_INPUT_INVALID)

    greenhouse = await get_from_cache(message.from_user.id)

    success = update_reference(
        greenhouse.id,
        await get_chosen_sensor(message.from_user.id),
        int(message.text[:6])
    )
    if not success:
        return await message.reply(MESSAGE_INPUT_INVALID)

    await message.reply(MESSAGE_INPUT_SUCCESS)
    return await command_reference(message, state)
