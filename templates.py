COMMAND_START = "start"
MESSAGE_START = "👋 Привет, {}!"

COMMAND_HELP = "/help"
MESSAGE_HELP = """
/menu - главное меню
/refresh - обновить данные
"""

BUTTON_BACK = "⬅️ Назад"

MESSAGE_INPUT_INVALID = "❌ Ошибка ввода."
MESSAGE_INPUT_SUCCESS = "✅ Ввод принят."

COMMAND_MENU = "menu"
MESSAGE_MENU = """
Демонстрационная теплица:

Сенсоры:
{}

Актуаторы:
{}
"""
MESSAGE_MENU_404 = "🙈 Теплица не найдена "
MESSAGE_SENSOR = "{}. {}: {} / {}"
MESSAGE_ACTUATOR = "{}. {}: {}"

COMMAND_REFRESH = "refresh"
BUTTON_REFRESH = "🔄 Обновить"

COMMAND_REFERENCE = "reference"
BUTTON_REFERENCE = "🌡️ Изменить эталон"
MESSAGE_REFERENCE_CHOOSING = "🎹 Выберите датчик:"
MESSAGE_REFERENCE_PENDING = "💬 Введите новое эталонное значение:"
MESSAGE_REFERENCE_INVALID_SENSOR = "❌ Неверный id сенсора."
BUTTON_REFERENCE_SENSOR = "{} {}"

EMOJIS_SENSORS = {
    "light": "💡",
    "temperature": "🌡️",
    "moisture": "💧",
    "lamp": "💡",
    "cooler": "❄️",
    "heater": "🔥",
    "shower": "💧"
}

EMOJIS_STATUSES = {
    False: "⚪",
    True: "🟢",
}
