from rest_framework.mixins import (
    CreateModelMixin,
    RetrieveModelMixin,
    UpdateModelMixin,
    ListModelMixin
)
from rest_framework.viewsets import GenericViewSet

from broker.models import Message
from devices.models import Device, Sensor, Actuator
from .pagination import OffsetPagination
from .serializers import (
    MessageSerializer,
    DeviceSerializer,
    SensorSerializer,
    ActuatorSerializer
)


class MessagesViewSet(GenericViewSet, CreateModelMixin, ListModelMixin):
    serializer_class = MessageSerializer
    pagination_class = OffsetPagination

    def get_queryset(self):
        return Message.objects.filter(topic=self.kwargs["topic"])

    def perform_create(self, serializer):
        serializer.save(topic=self.kwargs["topic"])


class DevicesViewSet(GenericViewSet, CreateModelMixin, RetrieveModelMixin):
    queryset = Device.objects.all()
    serializer_class = DeviceSerializer


class SensorsViewSet(GenericViewSet, CreateModelMixin, UpdateModelMixin):
    queryset = Sensor.objects.all()
    serializer_class = SensorSerializer


class ActuatorsViewSet(GenericViewSet, CreateModelMixin, UpdateModelMixin):
    queryset = Actuator.objects.all()
    serializer_class = ActuatorSerializer
