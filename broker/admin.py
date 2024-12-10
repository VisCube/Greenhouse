from django.contrib import admin

from .models import Message

class MessageAdmin(admin.ModelAdmin):
    list_display = ("id", "topic", "payload", "timestamp")
    list_editable = ("topic", "payload")
    list_filter = ("topic",)
    search_fields = ("payload",)


admin.site.register(Message, MessageAdmin)
