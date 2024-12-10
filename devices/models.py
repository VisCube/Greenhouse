from django.db import models


class Device(models.Model):
    pass


class Sensor(models.Model):
    TYPE_CHOICES = (
        ("light", "light"),
        ("temperature", "temperature"),
        ("moist", "moist"),
    )

    device = models.ForeignKey(
        Device,
        on_delete=models.CASCADE,
        related_name="sensors"
    )
    type = models.CharField(choices=TYPE_CHOICES, max_length=16)
    reading = models.FloatField(default=0)
    reference = models.FloatField(default=0)


class Actuator(models.Model):
    TYPE_CHOICES = (
        ("lamp", "lamp"),
        ("heater", "heater"),
        ("cooler", "cooler"),
        ("shower", "shower"),
    )

    device = models.ForeignKey(
        Device,
        on_delete=models.CASCADE,
        related_name="actuators"
    )
    type = models.CharField(choices=TYPE_CHOICES, max_length=16)
    status = models.BooleanField(default=False)

class Reading(models.Model):
    sensor = models.ForeignKey(
        Sensor,
        on_delete=models.CASCADE,
        related_name="readings"
    )
    value = models.FloatField()