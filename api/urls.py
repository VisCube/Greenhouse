from django.urls import include, path
from rest_framework import routers

from .views import (
    MessagesViewSet,
    DevicesViewSet,
    SensorsViewSet,
    ActuatorsViewSet
)

app_name = "api"

router = routers.DefaultRouter()
router.register(r"topics/(?P<topic>[\w-]+)", MessagesViewSet, "messages")
router.register(r"devices", DevicesViewSet, "devices")
router.register(r"sensors", SensorsViewSet, "sensors")
router.register(r"actuators", ActuatorsViewSet, "actuators")

urlpatterns = [
    path("", include(router.urls)),
]
