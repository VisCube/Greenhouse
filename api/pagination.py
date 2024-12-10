from rest_framework.pagination import LimitOffsetPagination
from rest_framework.response import Response


class OffsetPagination(LimitOffsetPagination):
    default_limit = 10

    def get_paginated_response(self, data):
        return Response({
            "count": len(data),
            "messages": data
        })