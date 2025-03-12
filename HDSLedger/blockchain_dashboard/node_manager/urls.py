
from django.urls import path
from . import views

urlpatterns = [
    path('', views.DashboardView.as_view(), name='dashboard'),
    path('start-node/<str:node_id>/', views.StartNodeView.as_view(), name='start_node'),
    path('stop-node/<str:node_id>/', views.StopNodeView.as_view(), name='stop_node'),
    path('start-client/<str:client_id>/', views.StartClientView.as_view(), name='start_client'),
    path('stop-client/<str:client_id>/', views.StopClientView.as_view(), name='stop_client'),
    path('logs/<str:process_type>/<str:process_id>/', views.LogsView.as_view(), name='logs'),
    path('start-all/', views.StartAllView.as_view(), name='start_all'),
    path('stop-all/', views.StopAllView.as_view(), name='stop_all'),
]