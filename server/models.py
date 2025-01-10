class Sensor:
    def __init__(self, id_: int, type_: str, reading: int, reference: int):
        self.id = id_
        self.type = type_
        self.reading = reading
        self.reference = reference


class Actuator:
    def __init__(self, id_: int, type_: str, status: bool):
        self.id = id_
        self.type = type_
        self.status = status


class Greenhouse:
    def __init__(self, id_: int, sensors: list[Sensor], actuators: list[Actuator]):
        self.id = id_
        self.sensors = sensors
        self.actuators = actuators
