from rest_framework import serializers

from broker.models import Message
from devices.models import Device, Sensor, Actuator


class MessageSerializer(serializers.ModelSerializer):
    class Meta:
        model = Message
        fields = "__all__"
        read_only_fields = ("topic",)


class SensorSerializer(serializers.ModelSerializer):
    class Meta:
        model = Sensor
        fields = "__all__"


class ActuatorSerializer(serializers.ModelSerializer):
    class Meta:
        model = Actuator
        fields = "__all__"


class DeviceSerializer(serializers.ModelSerializer):
    sensors = SensorSerializer(many=True, read_only=True)
    actuators = ActuatorSerializer(many=True, read_only=True)

    class Meta:
        model = Device
        fields = "__all__"
