from django.db import models


class Message(models.Model):
    topic = models.SlugField()
    payload = models.JSONField()
    timestamp = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return str(self.timestamp)
