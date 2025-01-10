import asyncio
import logging

from aiogram import Bot, Dispatcher
from aiogram.fsm.storage.memory import MemoryStorage

from config import config
from routers import common_router, menu, reference


async def main():
    logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(name)s - %(message)s")

    bot = Bot(config.bot_token.get_secret_value())
    dispatcher = Dispatcher(storage=MemoryStorage())
    dispatcher.include_routers(menu.router, reference.router, common_router)

    await bot.delete_webhook(drop_pending_updates=True)
    await dispatcher.start_polling(bot)


if __name__ == '__main__':
    asyncio.run(main())
