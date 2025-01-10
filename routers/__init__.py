from aiogram import Router
from aiogram.filters import Command
from aiogram.fsm.context import FSMContext
from aiogram.types import Message

from routers.menu import command_refresh
from templates import *

common_router = Router()


@common_router.message(Command("start"))
async def command_start(message: Message, state: FSMContext):
    await message.answer(MESSAGE_START.format(message.from_user.full_name))
    await command_refresh(message, state)


@common_router.message(Command("help"))
async def command_help(message: Message):
    await message.answer(MESSAGE_HELP)
